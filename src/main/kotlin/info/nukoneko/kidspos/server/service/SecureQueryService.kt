package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery

/**
 * Service for building secure, parameterized queries
 * Prevents SQL injection by using JPA Criteria API
 */
@Service
class SecureQueryService(
    private val entityManager: EntityManager
) {
    private val logger = LoggerFactory.getLogger(SecureQueryService::class.java)

    /**
     * Create a secure typed query with parameter binding
     */
    fun <T> createTypedQuery(
        entityClass: Class<T>,
        queryBuilder: (CriteriaBuilder, CriteriaQuery<T>) -> CriteriaQuery<T>
    ): TypedQuery<T> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(entityClass)

        val finalQuery = queryBuilder(criteriaBuilder, criteriaQuery)

        logger.debug("Creating secure typed query for entity: {}", entityClass.simpleName)
        return entityManager.createQuery(finalQuery)
    }

    /**
     * Sanitize input to prevent injection attacks
     * Removes potentially dangerous characters while preserving data integrity
     */
    fun sanitizeInput(input: String): String {
        // Remove SQL comment indicators
        var sanitized = input
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")

        // Remove common SQL injection patterns
        sanitized = sanitized.replace(Regex("\\b(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript)\\b", RegexOption.IGNORE_CASE), "")

        // Escape single quotes for string literals
        sanitized = sanitized.replace("'", "''")

        logger.debug("Sanitized input: Original='{}', Sanitized='{}'", input, sanitized)

        return sanitized
    }

    /**
     * Validate that numeric input is within expected bounds
     */
    fun validateNumericInput(value: Int, min: Int, max: Int): Int {
        if (value < min || value > max) {
            logger.warn("Numeric input validation failed: value={}, min={}, max={}", value, min, max)
            throw IllegalArgumentException("Numeric value out of bounds")
        }
        return value
    }

    /**
     * Validate that string input matches expected pattern
     */
    fun validatePattern(input: String, pattern: String): String {
        if (!input.matches(Regex(pattern))) {
            logger.warn("Pattern validation failed: input='{}', pattern='{}'", input, pattern)
            throw IllegalArgumentException("Input does not match expected pattern")
        }
        return input
    }

    /**
     * Create a safe LIKE pattern for queries
     */
    fun createSafeLikePattern(searchTerm: String, position: LikePosition = LikePosition.CONTAINS): String {
        // Escape special LIKE characters
        val escaped = searchTerm
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        return when (position) {
            LikePosition.STARTS_WITH -> "$escaped%"
            LikePosition.ENDS_WITH -> "%$escaped"
            LikePosition.CONTAINS -> "%$escaped%"
            LikePosition.EXACT -> escaped
        }
    }

    enum class LikePosition {
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS,
        EXACT
    }
}