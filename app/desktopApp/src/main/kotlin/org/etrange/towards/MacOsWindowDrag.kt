package org.etrange.towards

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Window
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val NsPoint = java.lang.foreign.MemoryLayout.structLayout(
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE,
)
private val NsRect = java.lang.foreign.MemoryLayout.structLayout(
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE,
)

/**
 * Moves a macOS window with [NSWindow.performWindowDragWithEvent:].
 * AppKit tracks the pointer itself, so the frame and the Metal layer stay together.
 * [Window.setLocation] on every move flashes that surface. The native call only
 * starts a drag while the mouse-down is still the window's current event, which
 * is inside `sendEvent:` and not afterwards.
 */
internal object MacOsWindowDrag {
    private val arena = Arena.ofAuto()
    private val objc = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", arena)
    private val linker = Linker.nativeLinker()

    private val getClass = downcall("objc_getClass", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS))
    private val objectGetClass = downcall("object_getClass", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS))
    private val registerSelector = downcall("sel_registerName", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS))
    private val getInstanceMethod = downcall(
        "class_getInstanceMethod",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val getImplementation = downcall(
        "method_getImplementation",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val setImplementation = downcall(
        "method_setImplementation",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val sendLong = downcall(
        "objc_msgSend",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val sendVoid = downcall(
        "objc_msgSend",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val sendVoidFlag = downcall(
        "objc_msgSend",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
    )
    private val sendPoint = downcall(
        "objc_msgSend",
        FunctionDescriptor.of(NsPoint, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )
    private val sendRect = downcall(
        "objc_msgSend",
        FunctionDescriptor.of(NsRect, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
    )

    private val frameSel = selector("frame")
    private val locationInWindow = selector("locationInWindow")
    private val styleMask = selector("styleMask")
    private val eventType = selector("type")
    private val clickCount = selector("clickCount")
    private val performWindowDrag = selector("performWindowDragWithEvent:")
    private val zoom = selector("zoom:")
    private val setOpaque = selector("setOpaque:")
    private val pressedMouseButtons = selector("pressedMouseButtons")
    private val nsEvent = classPointer("NSEvent")

    private val windows = ConcurrentHashMap.newKeySet<Long>()
    private val installed = AtomicBoolean(false)
    private val dragging = AtomicBoolean(false)
    private var sendEventStub: MemorySegment? = null
    private var originalSendEvent: MethodHandle? = null

    /** An opaque window does not flash the desktop through the title bar while it moves. */
    fun makeOpaque(window: Window) {
        try {
            val nsWindow = nsWindowPointer(window)
            if (nsWindow == 0L) return
            sendVoidFlag.invokeWithArguments(pointer(nsWindow), setOpaque, 1)
        } catch (error: Throwable) {
            report(error)
        }
    }

    fun installDragQueue(window: ComposeWindow) {
        val nsWindow = nsWindowPointer(window)
        if (nsWindow == 0L) return
        windows.add(nsWindow)
        if (!installed.compareAndSet(false, true)) return
        installSendEvent(nsWindow)
    }

    private fun installSendEvent(nsWindow: Long) {
        try {
            val windowClass = objectGetClass.invokeWithArguments(pointer(nsWindow)) as MemorySegment
            val method = getInstanceMethod.invokeWithArguments(windowClass, selector("sendEvent:")) as MemorySegment
            if (method.address() == 0L) return
            val callback = MethodHandles.lookup()
                .findVirtual(
                    MacOsWindowDrag::class.java,
                    "onSendEvent",
                    MethodType.methodType(
                        Void.TYPE,
                        MemorySegment::class.java,
                        MemorySegment::class.java,
                        MemorySegment::class.java,
                    ),
                )
                .bindTo(this)
            sendEventStub = linker.upcallStub(
                callback,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                arena,
            )
            val previous = getImplementation.invokeWithArguments(method) as MemorySegment
            originalSendEvent = linker.downcallHandle(
                previous,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            )
            setImplementation.invokeWithArguments(method, sendEventStub)
        } catch (error: Throwable) {
            report(error)
        }
    }

    /**
     * [AWTWindow_Normal sendEvent:] is the mouse-down AppKit is tracking.
     * Traffic-light clicks are in the leading corner and are left for AppKit.
     */
    @Suppress("unused")
    fun onSendEvent(window: MemorySegment, command: MemorySegment, event: MemorySegment) {
        val handled = try {
            !dragging.get() &&
                window.address() in windows &&
                (sendLong.invokeWithArguments(event, eventType) as Long) == LeftMouseDown &&
                dragTitleBar(window, event)
        } catch (error: Throwable) {
            report(error)
            false
        }
        if (!handled) {
            try {
                originalSendEvent?.invokeWithArguments(window, command, event)
            } catch (error: Throwable) {
                report(error)
            }
        }
    }

    /** @return true when this mouse-down started a drag or a zoom and must not reach Compose */
    private fun dragTitleBar(nsWindow: MemorySegment, event: MemorySegment): Boolean {
        if (isFullscreen(nsWindow)) return false
        val (x, y) = point(event, locationInWindow)
        val height = rectHeight(nsWindow, frameSel)
        if (y < height - TitleBandPoints) return false
        if (x < TrafficLightWidth) return false
        val clicks = sendLong.invokeWithArguments(event, clickCount) as Long
        if (clicks >= 2L) {
            sendVoid.invokeWithArguments(nsWindow, zoom, nsWindow)
            return true
        }
        val before = mouseButtons()
        dragging.set(true)
        try {
            sendVoid.invokeWithArguments(nsWindow, performWindowDrag, event)
        } finally {
            dragging.set(false)
        }
        return before != 0L && mouseButtons() == 0L
    }

    private fun isFullscreen(nsWindow: MemorySegment): Boolean {
        val mask = sendLong.invokeWithArguments(nsWindow, styleMask) as Long
        return mask and FullScreenMask != 0L
    }

    private fun point(receiver: MemorySegment, sel: MemorySegment): Pair<Double, Double> =
        Arena.ofConfined().use { scratch ->
            val segment = sendPoint.invokeWithArguments(scratch, receiver, sel) as MemorySegment
            segment.get(ValueLayout.JAVA_DOUBLE, 0) to segment.get(ValueLayout.JAVA_DOUBLE, 8)
        }

    private fun rectHeight(receiver: MemorySegment, sel: MemorySegment): Double =
        Arena.ofConfined().use { scratch ->
            val segment = sendRect.invokeWithArguments(scratch, receiver, sel) as MemorySegment
            segment.get(ValueLayout.JAVA_DOUBLE, 24)
        }

    private var reported = false

    private fun report(error: Throwable) {
        if (reported) return
        reported = true
        error.printStackTrace()
    }

    private fun mouseButtons(): Long =
        sendLong.invokeWithArguments(nsEvent, pressedMouseButtons) as Long

    private fun nsWindowPointer(window: Window): Long {
        val peerField = Class.forName("java.awt.Component").getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(window) ?: return 0L
        val platformWindow = peer.javaClass.getMethod("getPlatformWindow").invoke(peer) ?: return 0L
        var type: Class<*>? = platformWindow.javaClass
        while (type != null) {
            val ptr = type.declaredFields.firstOrNull { it.name == "ptr" }
            if (ptr != null) {
                ptr.isAccessible = true
                return ptr.getLong(platformWindow)
            }
            type = type.superclass
        }
        return 0L
    }

    private fun pointer(address: Long): MemorySegment = MemorySegment.ofAddress(address)

    private fun classPointer(name: String): MemorySegment =
        getClass.invokeWithArguments(arena.allocateFrom(name)) as MemorySegment

    private fun selector(name: String): MemorySegment =
        registerSelector.invokeWithArguments(arena.allocateFrom(name)) as MemorySegment

    private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle =
        linker.downcallHandle(objc.find(name).orElseThrow(), descriptor)
}

private const val FullScreenMask = 1L shl 14
private const val LeftMouseDown = 1L
private const val TrafficLightWidth = 78.0
private val TitleBandPoints = org.etrange.towards.ui.window.DesktopTitleBar.MacOs.height.value.toDouble()
