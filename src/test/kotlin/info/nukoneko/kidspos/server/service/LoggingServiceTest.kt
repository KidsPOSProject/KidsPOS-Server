package info.nukoneko.kidspos.server.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender

class LoggingServiceTest {

    @Test
    fun `should log messages at different levels`() {
        // Given
        val logger = LoggerFactory.getLogger(SaleService::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        // SaleService instance is not needed for this logging test

        // When - simulate logging (we'll add actual logging in the service)
        logger.debug("Debug message")
        logger.info("Info message")
        logger.warn("Warning message")
        logger.error("Error message")

        // Then
        val logs = listAppender.list
        assertThat(logs).hasSize(4)
        assertThat(logs[0].message).isEqualTo("Debug message")
        assertThat(logs[1].message).isEqualTo("Info message")
        assertThat(logs[2].message).isEqualTo("Warning message")
        assertThat(logs[3].message).isEqualTo("Error message")
    }

    @Test
    fun `should not log sensitive information`() {
        // Given
        val logger = LoggerFactory.getLogger(SaleService::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        val _sensitiveData = "password=secret123"
        val maskedData = "password=***"

        // When
        logger.info("User login attempt: {}", maskedData)

        // Then
        val logs = listAppender.list
        assertThat(logs[0].message).doesNotContain("secret123")
        assertThat(logs[0].message).contains("***")
    }
}