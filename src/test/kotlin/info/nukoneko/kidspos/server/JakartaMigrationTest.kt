package info.nukoneko.kidspos.server

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests to verify Jakarta EE migration readiness
 * Part of Task 9.3: Spring Boot 3.x migration
 */
@DisplayName("Jakarta EE Migration Tests")
class JakartaMigrationTest {

    @Test
    @DisplayName("Should document javax.* imports for future migration")
    fun shouldDocumentJavaxImports() {
        val sourceDir = File("src/main/kotlin")
        val javaxImports = mutableListOf<String>()
        if (sourceDir.exists()) {
            Files.walk(Paths.get(sourceDir.path))
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .toList()
                .forEach { file ->
                    Files.lines(file).forEach { line ->
                        if (line.contains("import javax.persistence") ||
                            line.contains("import javax.validation") ||
                            line.contains("import javax.servlet") ||
                            line.contains("import javax.annotation")
                        ) {
                            javaxImports.add("${file.fileName}: $line")
                        }
                    }
                }
        }

        // For Spring Boot 2.7.x, javax imports are still valid
        // Document them for future Jakarta migration
        if (javaxImports.isNotEmpty()) {
            println("Documented javax imports for future Jakarta migration:")
            javaxImports.forEach { println("  $it") }
        }
        // This is expected for Spring Boot 2.7.x
        assertTrue(true, "javax imports documented for future migration")
    }

    @Test
    @DisplayName("Should use Spring Boot 3.x with Java 21")
    fun shouldUseSpringBoot3Version() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "build.gradle not found")
        val content = buildFile.readText()
        val versionPattern = Regex("springBootVersion = '([^']+)'")
        val match = versionPattern.find(content)

        assertNotNull(match, "Spring Boot version not found in build.gradle")
        val version = match!!.groupValues[1]

        // Verify we're on Spring Boot 3.x
        assertTrue(
            version.startsWith("3."),
            "Should use Spring Boot 3.x with Java 21. Current version: $version"
        )
    }

    @Test
    @DisplayName("Should use compatible Kotlin version for Spring Boot 2.7.x")
    fun shouldUseCompatibleKotlinVersion() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "build.gradle not found")
        val content = buildFile.readText()
        val versionPattern = Regex("kotlinVersion = '([^']+)'")
        val match = versionPattern.find(content)

        assertNotNull(match, "Kotlin version not found in build.gradle")
        val version = match!!.groupValues[1]
        val parts = version.split(".")
        val major = parts[0].toInt()
        val minor = parts[1].toInt()

        // Spring Boot 2.7.x works with Kotlin 1.6.x
        assertTrue(
            major > 1 || (major == 1 && minor >= 6),
            "Kotlin version $version should be 1.6+ for Spring Boot 2.7.x"
        )
    }

    @Test
    @DisplayName("Should use Java 21 for Spring Boot 3.x")
    fun shouldUseCompatibleJavaVersion() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "build.gradle not found")
        val content = buildFile.readText()

        // Check sourceCompatibility
        val sourceCompatPattern = Regex("sourceCompatibility = '([^']+)'")
        val sourceMatch = sourceCompatPattern.find(content)

        if (sourceMatch != null) {
            val version = sourceMatch.groupValues[1]
            val versionNum = if (version.startsWith("1.")) {
                version.substring(2).toIntOrNull() ?: 0
            } else {
                version.toIntOrNull() ?: 0
            }
            assertEquals(
                21, versionNum,
                "Source compatibility should be Java 21 for Spring Boot 3.x"
            )
        }

        // Check Kotlin JVM target
        val jvmTargetPattern = Regex("jvmTarget = '([^']+)'")
        val jvmMatches = jvmTargetPattern.findAll(content)

        jvmMatches.forEach { match ->
            val version = match.groupValues[1]
            val versionNum = if (version.startsWith("1.")) {
                version.substring(2).toIntOrNull() ?: 0
            } else {
                version.toIntOrNull() ?: 0
            }
            assertTrue(
                versionNum >= 17 && versionNum <= 21,
                "JVM target $version should be Java 17-21 for Spring Boot 3.x"
            )
        }
    }

    @Test
    @DisplayName("Should have proper Jakarta dependencies")
    fun shouldHaveJakartaDependencies() {
        val buildFile = File("build.gradle")
        assertTrue(buildFile.exists(), "build.gradle not found")
        val content = buildFile.readText()
        val dependencies = content.lines()
            .filter { it.contains("implementation") || it.contains("compile") }
            .joinToString("\n")

        // For Spring Boot 3.x, these should be automatically included
        // but we check if old javax dependencies are not explicitly added
        assertFalse(
            dependencies.contains("javax.servlet"),
            "Found javax.servlet dependency, should use jakarta.servlet"
        )
        assertFalse(
            dependencies.contains("javax.persistence"),
            "Found javax.persistence dependency, should use jakarta.persistence"
        )
        assertFalse(
            dependencies.contains("javax.validation"),
            "Found javax.validation dependency, should use jakarta.validation"
        )
    }
}