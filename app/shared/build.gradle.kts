import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("15.5")
    }
    
    jvm()
    
    android {
       namespace = "org.etrange.towards.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.mp.client.okhttp)
            implementation(libs.maplibre.compose.runtime.opengl.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.mp.client.darwin)
        }
        jvmMain {
            resources.srcDir(layout.buildDirectory.dir("generated/location-natives"))
            dependencies {
                implementation(libs.ktor.mp.client.cio)
                implementation(libs.jna)
                implementation(libs.dbus.java.core)
                implementation(libs.dbus.java.transport)
                val maplibreComposeVersion = libs.versions.maplibre.compose.get()
                runtimeOnly("org.maplibre.compose:${maplibreDesktopRuntime()}:$maplibreComposeVersion")
            }
        }
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.mp.client.core)
            implementation(libs.ktor.mp.client.content.negotiation)
            implementation(libs.ktor.mp.client.serialization.json)
            implementation(libs.maplibre.compose)
            implementation(libs.multiplatform.settings.no.arg)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

private fun maplibreDesktopRuntime(): String {
    val hostOs = when (val os = System.getProperty("os.name").lowercase()) {
        "mac os x" -> "macos"
        else -> os.split(" ").first()
    }
    val hostArch = when (val arch = System.getProperty("os.arch").lowercase()) {
        "x86_64", "amd64" -> "x64"
        "aarch64", "arm64" -> "arm64"
        else -> arch
    }
    val renderer = when (hostOs) {
        "macos" -> "metal"
        else -> "vulkan"
    }
    return "maplibre-compose-runtime-$renderer-$hostOs-$hostArch"
}

private val locationHostOs = providers.systemProperty("os.name").map { os ->
    val name = os.lowercase()
    when {
        name.startsWith("mac") -> "macos"
        name.startsWith("windows") -> "windows"
        name.startsWith("linux") -> "linux"
        else -> name.substringBefore(' ')
    }
}

private val locationHostArch = providers.systemProperty("os.arch").map { arch ->
    when (arch.lowercase()) {
        "x86_64", "amd64" -> "x64"
        "aarch64", "arm64" -> "arm64"
        else -> arch.lowercase()
    }
}

private val locationNativeDir = layout.buildDirectory.dir(
    locationHostOs.zip(locationHostArch) { os, arch ->
        "generated/location-natives/natives/$os-$arch"
    },
)

val prepareLocationNativeResources = tasks.register("prepareLocationNativeResources") {
    val dir = layout.buildDirectory.dir("generated/location-natives")
    outputs.dir(dir)
    doLast { dir.get().asFile.mkdirs() }
}

val compileLocationShim = tasks.register<CompileLocationShimTask>("compileLocationShim") {
    hostOs.set(locationHostOs)
    hostArch.set(locationHostArch)
    cHeader.set(layout.projectDirectory.file("native/location.h"))
    macSource.set(layout.projectDirectory.file("native/macos/location.m"))
    helperSource.set(layout.projectDirectory.file("native/macos/location-helper.m"))
    helperPlist.set(layout.projectDirectory.file("native/macos/LocationHelper-Info.plist"))
    windowsSource.set(layout.projectDirectory.file("native/windows/location.cpp"))
    outputDirectory.set(locationNativeDir)
    dependsOn(prepareLocationNativeResources)
    onlyIf {
        val os = hostOs.get()
        os == "macos" || os == "windows"
    }
}

tasks.named("jvmProcessResources").configure {
    dependsOn(prepareLocationNativeResources, compileLocationShim)
}

abstract class CompileLocationShimTask : DefaultTask() {
    @get:Input
    abstract val hostOs: Property<String>

    @get:Input
    abstract val hostArch: Property<String>

    @get:InputFile
    abstract val cHeader: RegularFileProperty

    @get:InputFile
    abstract val macSource: RegularFileProperty

    @get:InputFile
    abstract val helperSource: RegularFileProperty

    @get:InputFile
    abstract val helperPlist: RegularFileProperty

    @get:InputFile
    abstract val windowsSource: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun compile() {
        val output = outputDirectory.get().asFile
        output.mkdirs()
        when (hostOs.get()) {
            "macos" -> {
                compileMac(
                    macSource.get().asFile,
                    cHeader.get().asFile.parentFile,
                    output.resolve("libtowards-location.dylib"),
                )
                compileMacHelper(
                    helperSource.get().asFile,
                    helperPlist.get().asFile,
                    output.resolve("TowardsLocation.app"),
                )
            }
            "windows" -> compileWindows(
                windowsSource.get().asFile,
                output.resolve("towards-location.dll"),
                hostArch.get(),
            )
            else -> error("The location shim is only compiled on macOS and Windows")
        }
    }

    private fun compileMacHelper(source: File, plist: File, app: File) {
        val binary = app.resolve("Contents/MacOS/location-helper")
        binary.parentFile.mkdirs()
        app.resolve("Contents").mkdirs()
        plist.copyTo(app.resolve("Contents/Info.plist"), overwrite = true)
        runCommand(
            listOf(
                "clang",
                "-fobjc-arc",
                "-mmacosx-version-min=11.0",
                "-framework",
                "Foundation",
                "-framework",
                "AppKit",
                "-framework",
                "CoreLocation",
                source.absolutePath,
                "-o",
                binary.absolutePath,
            ),
            "Failed to compile the macOS location helper. Install the Xcode Command Line Tools.",
        )
    }

    private fun compileMac(source: File, headerDir: File, output: File) {
        runCommand(
            listOf(
                "clang",
                "-dynamiclib",
                "-fobjc-arc",
                "-mmacosx-version-min=11.0",
                "-I",
                headerDir.absolutePath,
                "-framework",
                "Foundation",
                "-framework",
                "CoreLocation",
                source.absolutePath,
                "-o",
                output.absolutePath,
            ),
            "Failed to compile the macOS location shim. Install the Xcode Command Line Tools.",
        )
    }

    private fun compileWindows(source: File, output: File, arch: String) {
        val vswhere = listOf(
            File(System.getenv("ProgramFiles(x86)") ?: "", "Microsoft Visual Studio/Installer/vswhere.exe"),
            File(System.getenv("ProgramFiles") ?: "", "Microsoft Visual Studio/Installer/vswhere.exe"),
        ).firstOrNull { it.isFile }
            ?: error(
                "Visual Studio Build Tools are required to compile the Windows location shim " +
                    "(vswhere.exe was not found).",
            )
        val cl = vswhereFind(vswhere, "VC/Tools/MSVC/**/bin/Hostx64/$arch/cl.exe")
            ?: vswhereFind(vswhere, "VC/Tools/MSVC/**/bin/Host$arch/$arch/cl.exe")
            ?: error("cl.exe was not found. Install the MSVC toolset for $arch.")
        val sdkRoot = listOf(
            File(System.getenv("ProgramFiles(x86)") ?: "", "Windows Kits/10"),
            File(System.getenv("ProgramFiles") ?: "", "Windows Kits/10"),
        ).firstOrNull { File(it, "Include").isDirectory }
            ?: error("The Windows SDK was not found. Install the Windows SDK with C++/WinRT headers.")
        val sdkVersion = File(sdkRoot, "Include").listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("10.") }
            ?.maxByOrNull { it.name }
            ?.name
            ?: error("The Windows SDK include directory is empty.")
        val cppwinrt = File(sdkRoot, "Include/$sdkVersion/cppwinrt")
        if (!cppwinrt.isDirectory) {
            error("C++/WinRT headers were not found at ${cppwinrt.path}. Install that Windows SDK component.")
        }
        val msvcRoot = cl.parentFile.parentFile.parentFile.parentFile
        val includes = listOf(
            cppwinrt,
            File(sdkRoot, "Include/$sdkVersion/ucrt"),
            File(sdkRoot, "Include/$sdkVersion/shared"),
            File(sdkRoot, "Include/$sdkVersion/um"),
            File(msvcRoot, "include"),
        )
        val libraries = listOf(
            File(sdkRoot, "Lib/$sdkVersion/ucrt/$arch"),
            File(sdkRoot, "Lib/$sdkVersion/um/$arch"),
            File(msvcRoot, "lib/$arch"),
        )
        val command = mutableListOf(
            cl.absolutePath,
            "/nologo",
            "/std:c++17",
            "/EHsc",
            "/MD",
            "/LD",
            "/O2",
            "/bigobj",
            "/DNDEBUG",
            "/DUNICODE",
            "/D_UNICODE",
            "/DWIN32",
            "/D_WINDOWS",
        )
        includes.forEach { include ->
            command += "/I"
            command += include.absolutePath
        }
        command += source.absolutePath
        command += "/link"
        command += "/OUT:${output.absolutePath}"
        libraries.forEach { library ->
            command += "/LIBPATH:${library.absolutePath}"
        }
        command += "windowsapp.lib"
        runCommand(command, "Failed to compile the Windows location shim.")
    }

    private fun vswhereFind(vswhere: File, pattern: String): File? {
        val output = try {
            runCommand(
                listOf(
                    vswhere.absolutePath,
                    "-latest",
                    "-products",
                    "*",
                    "-requires",
                    "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
                    "-find",
                    pattern,
                ),
                "vswhere failed while looking for the MSVC toolset.",
            )
        } catch (_: IllegalStateException) {
            return null
        }
        return output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.let(::File)
            ?.takeIf { it.isFile }
    }

    private fun runCommand(command: List<String>, failure: String): String {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (error: Exception) {
            error("$failure ${error.message}")
        }
        val text = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) {
            error(buildString {
                append(failure)
                append(" (exit ")
                append(exit)
                append(")")
                if (text.isNotBlank()) {
                    append("\n")
                    append(text.trim())
                }
            })
        }
        return text
    }
}
