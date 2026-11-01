package trace.connect

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.asSequence

enum class BuildSystem { GRADLE, MAVEN, NONE }

object ProjectFiles {
    private val PACKAGE_REGEX = Regex("""(?m)^\s*package\s+([A-Za-z_][\w.]*)""")

    /** Main source roots (src/main/java, src/main/kotlin) anywhere under the project, shallowest first. */
    fun sourceRoots(project: Path): List<Path> {
        if (!project.isDirectory()) return emptyList()
        val roots = mutableListOf<Path>()
        Files.walk(project, 7).use { stream ->
            stream.asSequence()
                .filter { it.isDirectory() }
                .filter { path ->
                    val n = path.name
                    (n == "java" || n == "kotlin") && path.parent?.name == "main" && path.parent?.parent?.name == "src"
                }
                .forEach { roots.add(it) }
        }
        return roots.sortedBy { it.nameCount }
    }

    /**
     * Infers the allow-list package prefix from source package declarations:
     * the most common first-two-segment prefix. Returns null when no package
     * statements are found (caller then requires an explicit --allow).
     */
    fun detectAllow(project: Path): String? {
        val counts = mutableMapOf<String, Int>()
        var scanned = 0
        for (root in sourceRoots(project)) {
            Files.walk(root).use { stream ->
                stream.asSequence()
                    .filter { Files.isRegularFile(it) && (it.name.endsWith(".java") || it.name.endsWith(".kt")) }
                    .take(400)
                    .forEach { file ->
                        if (scanned >= 400) return@forEach
                        scanned++
                        val pkg = PACKAGE_REGEX.find(Files.readString(file))?.groupValues?.get(1) ?: return@forEach
                        val prefix = pkg.split(".").take(2).joinToString(".")
                        counts[prefix] = (counts[prefix] ?: 0) + 1
                    }
            }
        }
        return counts.maxByOrNull { it.value }?.key
    }

    fun buildSystem(project: Path): BuildSystem = when {
        Files.isRegularFile(project.resolve("gradlew")) || Files.isRegularFile(project.resolve("gradlew.bat")) ||
            Files.isRegularFile(project.resolve("build.gradle")) || Files.isRegularFile(project.resolve("build.gradle.kts")) ->
            BuildSystem.GRADLE
        Files.isRegularFile(project.resolve("pom.xml")) -> BuildSystem.MAVEN
        else -> BuildSystem.NONE
    }
}
