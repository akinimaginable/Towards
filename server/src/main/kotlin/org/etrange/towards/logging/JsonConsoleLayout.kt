package org.etrange.towards.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.LayoutBase
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Minimal JSON layout for Logback so stdout can be parsed by log aggregators
 * without introducing an extra encoder dependency.
 */
class JsonConsoleLayout : LayoutBase<ILoggingEvent>() {
    override fun doLayout(event: ILoggingEvent): String {
        val builder = StringBuilder(256)
        builder.append('{')
        appendField(builder, "timestamp", TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.timeStamp)), first = true)
        appendField(builder, "level", event.level?.levelStr.orEmpty())
        appendField(builder, "logger", event.loggerName.orEmpty())
        appendField(builder, "thread", event.threadName.orEmpty())
        appendField(builder, "message", event.formattedMessage.orEmpty())

        val mdc = runCatching { event.mdcPropertyMap }
            .getOrNull()
            .orEmpty()
        for ((key, value) in mdc) {
            if (value != null) {
                appendField(builder, key, value)
            }
        }

        val throwableProxy = event.throwableProxy
        if (throwableProxy != null) {
            appendField(builder, "exception", ThrowableProxyUtil.asString(throwableProxy))
        }

        builder.append('}')
        builder.append(System.lineSeparator())
        return builder.toString()
    }

    private fun appendField(
        builder: StringBuilder,
        key: String,
        value: String,
        first: Boolean = false,
    ) {
        if (!first) {
            builder.append(',')
        }
        builder.append('"')
        escape(builder, key)
        builder.append('"').append(':').append('"')
        escape(builder, value)
        builder.append('"')
    }

    private fun escape(builder: StringBuilder, value: String) {
        for (character in value) {
            when (character) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> if (character < ' ') {
                    builder.append("\\u")
                    builder.append(character.code.toString(16).padStart(4, '0'))
                } else {
                    builder.append(character)
                }
            }
        }
    }

    companion object {
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
    }
}
