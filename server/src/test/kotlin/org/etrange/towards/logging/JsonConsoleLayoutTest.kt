package org.etrange.towards.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class JsonConsoleLayoutTest {
    @Test
    fun emitsParsableJsonWithEscapedMessage() {
        val context = LoggerContext()
        val layout = JsonConsoleLayout().apply {
            this.context = context
            start()
        }
        val appender = ListAppender<ILoggingEvent>().apply {
            this.context = context
            start()
        }
        val logger = context.getLogger("org.etrange.towards.test") as Logger
        logger.level = Level.INFO
        logger.addAppender(appender)
        logger.info("""hello "world" """)

        val line = layout.doLayout(appender.list.single()).trim()

        assertTrue(line.startsWith("{") && line.endsWith("}"))
        assertContains(line, """"level":"INFO"""")
        assertContains(line, """"logger":"org.etrange.towards.test"""")
        assertContains(line, """"message":"hello \"world\" """")
        assertContains(line, """"timestamp":""")
    }
}
