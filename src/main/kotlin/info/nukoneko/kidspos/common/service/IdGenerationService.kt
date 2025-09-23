package info.nukoneko.kidspos.common.service

import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service

/**
 * Service for generating unique IDs for entities
 *
 * This service provides a centralized way to generate sequential IDs
 * for any repository that implements the getLastId() method.
 */
@Service
class IdGenerationService {
    private val logger = LoggerFactory.getLogger(IdGenerationService::class.java)

    /**
     * Generates the next sequential ID for a given repository
     *
     * @param repository Any repository with a getLastId() method
     * @return The next available ID (lastId + 1, or 1 if empty)
     */
    fun <T> generateNextId(repository: T): Int {
        return try {
            val lastId = when (repository) {
                is HasLastId -> repository.getLastId()
                else -> {
                    // Use reflection to call getLastId() if available
                    val method = repository!!.javaClass.getMethod("getLastId")
                    method.invoke(repository) as Int
                }
            }
            val nextId = lastId + 1
            logger.debug("Generated next ID: {} for repository: {}", nextId, repository.javaClass.simpleName)
            nextId
        } catch (e: EmptyResultDataAccessException) {
            logger.debug("No existing records found, starting with ID: 1")
            1
        } catch (e: Exception) {
            logger.warn("Error generating ID, defaulting to 1: {}", e.message)
            1
        }
    }

    /**
     * Interface for repositories that support getLastId
     */
    interface HasLastId {
        fun getLastId(): Int
    }
}