package info.nukoneko.kidspos.server.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Base class for services with logging support
 *
 * Provides common functionality for all service classes including
 * pre-configured logger instance that automatically uses the concrete
 * subclass name for proper log categorization.
 */
abstract class BaseService {
    protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)
}