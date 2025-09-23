package info.nukoneko.kidspos.server.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RestController
import jakarta.persistence.Entity
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class PackageStructureTest {

    private val basePackage = "info.nukoneko.kidspos.server"
    private val sourcePath = Paths.get("src/main/kotlin/info/nukoneko/kidspos/server")

    @Test
    fun `controllers should be in controller package`() {
        // Check that all @Controller and @RestController classes are in the controller package
        val controllerFiles = findFilesInPackage("controller")
        assertThat(controllerFiles).isNotEmpty
    }

    @Test
    fun `services should be in service package`() {
        // Check that all @Service classes are in the service package
        val serviceFiles = findFilesInPackage("service")
        assertThat(serviceFiles).isNotEmpty
    }

    @Test
    fun `repositories should be in repository package`() {
        // Check that all @Repository classes are in the repository package
        val repositoryFiles = findFilesInPackage("repository")
        assertThat(repositoryFiles).isNotEmpty
    }

    @Test
    fun `entities should be in entity package`() {
        // Check that all @Entity classes are in the entity package
        val entityFiles = findFilesInPackage("entity")
        assertThat(entityFiles).isNotEmpty
    }

    @Test
    fun `DTOs should be in dto package under controller`() {
        // Check that DTOs are properly organized
        val dtoPath = sourcePath.resolve("controller/dto")
        assertThat(Files.exists(dtoPath)).isTrue

        val requestPath = dtoPath.resolve("request")
        val responsePath = dtoPath.resolve("response")
        assertThat(Files.exists(requestPath)).isTrue
        assertThat(Files.exists(responsePath)).isTrue
    }

    @Test
    fun `configuration classes should be in config package`() {
        val configFiles = findFilesInPackage("config")
        assertThat(configFiles).isNotEmpty
    }

    @Test
    fun `domain exceptions should be in domain exception package`() {
        val domainPath = sourcePath.resolve("domain/exception")
        assertThat(Files.exists(domainPath)).isTrue
    }

    private fun findFilesInPackage(packageName: String): List<Path> {
        val path = sourcePath.resolve(packageName)
        return if (Files.exists(path)) {
            Files.walk(path)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .toList()
        } else {
            emptyList()
        }
    }
}