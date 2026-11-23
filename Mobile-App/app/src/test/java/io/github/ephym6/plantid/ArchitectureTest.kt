package io.github.ephym6.plantid

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the boundaries that keep the Sprint 7 model swap a one-line change
 * (DECISIONS D5, D6):
 * - only the dependency container (`di/`) creates concrete implementations,
 *   including the demo identifier, so the UI can't depend on the mock;
 * - `domain/` stays plain Kotlin: no Android, UI, data or DI imports.
 */
class ArchitectureTest {

    private val sourceRoot = File("src/main/java/io/github/ephym6/plantid")

    private val concreteImplementations = listOf(
        "MockIdentificationService",
        "ModelIdentificationService",
        "CachePhotoStore",
        "AssetPlantRepository",
        "RoomHistoryRepository",
        "PlantIdDatabase",
        "HistoryPhotoStore",
    )

    private fun sources(dir: String = ""): List<File> =
        File(sourceRoot, dir).walkTopDown().filter { it.extension == "kt" }.toList()

    private fun File.relative() = relativeTo(sourceRoot).invariantSeparatorsPath

    /** Code lines only: KDoc and comments may mention implementations. */
    private fun File.codeLines() = readLines().filterNot { line ->
        line.trimStart().let { it.startsWith("*") || it.startsWith("/*") || it.startsWith("//") }
    }

    @Test
    fun `source tree is where the test expects`() {
        assertTrue("missing $sourceRoot", sources().size > 20)
    }

    @Test
    fun `only di and data reference concrete implementations`() {
        val violations = sources().filterNot { it.relative().startsWith("di/") || it.relative().startsWith("data/") }
            .flatMap { file ->
                file.codeLines().filter { line -> concreteImplementations.any { Regex("\\b$it\\b").containsMatchIn(line) } }
                    .map { "${file.relative()}: ${it.trim()}" }
            }
        assertTrue("Concrete implementations used outside di/:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `domain has no Android, UI, data or DI dependencies`() {
        val forbidden = Regex("^import (android\\.|androidx\\.|io\\.github\\.ephym6\\.plantid\\.(ui|data|di)\\.)")
        val violations = sources("domain").flatMap { file ->
            file.readLines().filter { forbidden.containsMatchIn(it) }.map { "${file.relative()}: $it" }
        }
        assertTrue("domain/ must stay plain Kotlin:\n" + violations.joinToString("\n"), violations.isEmpty())
    }
}
