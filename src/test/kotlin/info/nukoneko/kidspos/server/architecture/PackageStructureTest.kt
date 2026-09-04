package info.nukoneko.kidspos.server.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence

/**
 * レイヤの置き場所が崩れていないことを検証する
 *
 * ディレクトリの有無ではなく、アノテーションの付いたクラスが所定のパッケージに
 * あるかどうかを見る。
 */
class PackageStructureTest {
    private val sourcePath: Path = Paths.get("src/main/kotlin/info/nukoneko/kidspos")

    @Test
    fun `services should be in the service package`() {
        assertThat(misplaced("@Service", "server/service")).isEmpty()
    }

    @Test
    fun `repositories should be in the repository package`() {
        assertThat(misplaced("@Repository", "server/repository")).isEmpty()
    }

    @Test
    fun `entities should be in the entity package`() {
        assertThat(misplaced("@Entity", "server/entity")).isEmpty()
    }

    @Test
    fun `controllers should be in the controller package`() {
        assertThat(misplaced("@RestController", "server/controller")).isEmpty()
        assertThat(misplaced("@Controller", "server/controller")).isEmpty()
    }

    @Test
    fun `mappers should not depend on repositories directly`() {
        val offenders =
            kotlinFiles()
                .filter { it.contains(Paths.get("service/mapper")) }
                .filter { Files.readString(it).contains(".repository.") }
                .map { it.fileName.toString() }
                .toList()

        assertThat(offenders).isEmpty()
    }

    private fun misplaced(
        annotation: String,
        expectedPackage: String,
    ): List<String> {
        val expected = sourcePath.resolve(expectedPackage)
        return kotlinFiles()
            .filter { Files.readString(it).lineSequence().any { line -> line.trimEnd() == annotation } }
            .filter { !it.startsWith(expected) }
            .map { sourcePath.relativize(it).toString() }
            .toList()
    }

    private fun kotlinFiles(): Sequence<Path> =
        Files
            .walk(sourcePath)
            .asSequence()
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
}
