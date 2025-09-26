package info.nukoneko.kidspos.server

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests to verify Gradle build system optimizations
 * Part of Task 9.4: Gradle build system optimization
 */
@DisplayName("Gradle Build Optimization Tests")
class GradleOptimizationTest {
    @Test
    @DisplayName("Should use latest stable Gradle version")
    fun shouldUseLatestStableGradleVersion() {
        val wrapperFile = File("gradle/wrapper/gradle-wrapper.properties")
        assertTrue(wrapperFile.exists(), "Gradle wrapper properties not found")

        val content = wrapperFile.readText()
        val versionPattern = Regex("gradle-([0-9.]+)-")
        val match = versionPattern.find(content)

        assertNotNull(match, "Gradle version not found in wrapper properties")
        val version = match!!.groupValues[1]
        val versionParts = version.split(".")
        val majorVersion = versionParts[0].toIntOrNull() ?: 0
        val minorVersion = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

        // Gradle 8.5 or newer for optimal Spring Boot 3.x support
        val isValidVersion = majorVersion > 8 || (majorVersion == 8 && minorVersion >= 5)
        assertTrue(
            isValidVersion,
            "Should use Gradle 8.5 or newer. Current: $version",
        )
    }

    @Test
    @DisplayName("Should use version catalog for dependency management")
    fun shouldUseVersionCatalog() {
        val catalogFile = File("gradle/libs.versions.toml")
        assertTrue(
            catalogFile.exists(),
            "Version catalog (libs.versions.toml) should exist for centralized dependency management",
        )

        val content = catalogFile.readText()

        // Check for proper sections
        assertTrue(content.contains("[versions]"), "Should have versions section")
        assertTrue(content.contains("[libraries]"), "Should have libraries section")
        assertTrue(content.contains("[plugins]"), "Should have plugins section")

        // Check for key dependencies
        assertTrue(content.contains("spring-boot"), "Should define Spring Boot version")
        assertTrue(content.contains("kotlin"), "Should define Kotlin version")
    }

    @Test
    @DisplayName("Should have optimized build configuration")
    fun shouldHaveOptimizedBuildConfiguration() {
        val buildFile = File("build.gradle.kts")
        if (!buildFile.exists()) {
            // Check for Groovy DSL if Kotlin DSL doesn't exist
            val groovyBuildFile = File("build.gradle")
            assertTrue(groovyBuildFile.exists(), "Build file not found")
            return // Skip Kotlin DSL specific checks
        }

        val content = buildFile.readText()

        // Check for build optimization configurations
        assertTrue(
            content.contains("parallel = true") ||
                content.contains("org.gradle.parallel=true"),
            "Should enable parallel execution for faster builds",
        )

        // Check for configuration cache usage (Gradle 6.5+)
        val gradleProperties = File("gradle.properties")
        if (gradleProperties.exists()) {
            val properties = gradleProperties.readText()
            assertTrue(
                properties.contains("org.gradle.configuration-cache=true") ||
                    properties.contains("org.gradle.unsafe.configuration-cache=true"),
                "Should enable configuration cache for faster builds",
            )
        }
    }

    @Test
    @DisplayName("Should use build cache for incremental builds")
    fun shouldUseBuildCache() {
        val gradleProperties = File("gradle.properties")

        if (gradleProperties.exists()) {
            val content = gradleProperties.readText()
            assertTrue(
                content.contains("org.gradle.caching=true"),
                "Should enable build cache for incremental builds",
            )
        } else {
            // If gradle.properties doesn't exist, create it with optimizations
            fail("gradle.properties should exist with build optimizations")
        }
    }

    @Test
    @DisplayName("Should configure JVM arguments for optimal performance")
    fun shouldConfigureOptimalJvmArgs() {
        val gradleProperties = File("gradle.properties")

        if (gradleProperties.exists()) {
            val content = gradleProperties.readText()

            // Check for JVM memory settings
            assertTrue(
                content.contains("org.gradle.jvmargs") &&
                    content.contains("-Xmx"),
                "Should configure JVM heap size for better performance",
            )

            // Check for daemon mode
            assertTrue(
                content.contains("org.gradle.daemon=true"),
                "Should enable Gradle daemon for faster builds",
            )
        }
    }

    @Test
    @DisplayName("Should use type-safe project accessors")
    fun shouldUseTypeSafeAccessors() {
        val settingsFile = File("settings.gradle.kts")
        if (!settingsFile.exists()) {
            val groovySettings = File("settings.gradle")
            assertTrue(groovySettings.exists(), "Settings file should exist")
            return // Skip Kotlin DSL specific checks
        }

        val content = settingsFile.readText()
        assertTrue(
            content.contains("enableFeaturePreview(\"TYPESAFE_PROJECT_ACCESSORS\")"),
            "Should enable type-safe project accessors for multi-module builds",
        )
    }

    @Test
    @DisplayName("Should minimize dependency resolution time")
    fun shouldMinimizeDependencyResolution() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "Build file not found")

        val content = buildFile.readText()

        // Check that repositories are properly ordered (faster ones first)
        val repoSection =
            content
                .substringAfter("repositories {")
                .substringBefore("}")

        val mavenCentralIndex = repoSection.indexOf("mavenCentral()")
        assertTrue(mavenCentralIndex >= 0, "Should use mavenCentral repository")

        // Ensure no unnecessary repositories
        assertFalse(
            repoSection.contains("jcenter()"),
            "Should not use deprecated jcenter repository",
        )
    }

    @Test
    @DisplayName("Should have task configuration avoidance")
    fun shouldUseTaskConfigurationAvoidance() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "Build file not found")

        val content = buildFile.readText()

        // Check for lazy configuration patterns
        assertTrue(
            content.contains("tasks.withType") ||
                content.contains("tasks.named") ||
                content.contains("tasks.register"),
            "Should use task configuration avoidance patterns",
        )
    }
}
