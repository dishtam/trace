package trace.eval

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/** What one runner may call. The harness swaps this; everything else is shared. */
interface InvestigationTools : AutoCloseable {
    fun defs(): List<ToolDef>
    fun call(name: String, arguments: JsonObject): String
    override fun close() {}
}

internal fun objSchema(properties: Map<String, Pair<String, String>>, required: List<String>): JsonObject =
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            properties.forEach { (name, spec) ->
                putJsonObject(name) { put("type", spec.first); put("description", spec.second) }
            }
        }
        putJsonArray("required") { required.forEach { add(it) } }
    }

internal fun JsonObject.argString(name: String): String? = (this[name] as? JsonPrimitive)?.content
internal fun JsonObject.argLong(name: String): Long? = (this[name] as? JsonPrimitive)?.content?.toLongOrNull()

/**
 * Grep-only baseline tools (evaluation.md): search, read, list — confined to
 * the demo sources. Nothing outside the root is reachable, so ground-truth
 * fixtures and trace data cannot leak into a baseline investigation.
 */
class BaselineTools(private val sourceRoot: Path) : InvestigationTools {
    init { require(sourceRoot.isDirectory()) { "baseline source root missing: $sourceRoot" } }

    override fun defs(): List<ToolDef> = listOf(
        ToolDef("search_text", "Search the codebase for a literal string or regex. Returns matching file:line rows (max $MAX_MATCHES).",
            objSchema(mapOf("pattern" to ("string" to "text or regex to search for"),
                "path_glob" to ("string" to "optional file-name glob, e.g. *.java")), listOf("pattern"))),
        ToolDef("read_file", "Read a source file (path relative to the project root). Output is capped.",
            objSchema(mapOf("path" to ("string" to "relative file path"),
                "start_line" to ("integer" to "optional 1-based start line")), listOf("path"))),
        ToolDef("list_files", "List files under a directory (path relative to the project root).",
            objSchema(mapOf("path" to ("string" to "relative directory, empty for root")), emptyList())),
    )

    override fun call(name: String, arguments: JsonObject): String = when (name) {
        "search_text" -> search(arguments.argString("pattern") ?: "", arguments.argString("path_glob"))
        "read_file" -> read(arguments.argString("path") ?: "", arguments.argLong("start_line")?.toInt() ?: 1)
        "list_files" -> list(arguments.argString("path") ?: "")
        else -> "ERROR: unknown tool $name"
    }

    private fun resolve(relative: String): Path? {
        val resolved = sourceRoot.resolve(relative).normalize()
        return if (resolved.startsWith(sourceRoot.normalize())) resolved else null
    }

    private fun search(pattern: String, glob: String?): String {
        if (pattern.isBlank()) return "ERROR: pattern is empty"
        val regex = try { Regex(pattern) } catch (e: Exception) { Regex(Regex.escape(pattern)) }
        val matches = mutableListOf<String>()
        Files.walk(sourceRoot).asSequence()
            .filter { Files.isRegularFile(it) && (glob == null || it.name.matches(Regex(glob.replace("*", ".*")))) }
            .sortedBy { it.relativeTo(sourceRoot).toString() }
            .forEach { file ->
                if (matches.size >= MAX_MATCHES) return@forEach
                runCatching { file.readLines() }.getOrNull()?.forEachIndexed { index, line ->
                    if (matches.size < MAX_MATCHES && regex.containsMatchIn(line)) {
                        matches += "${file.relativeTo(sourceRoot)}:${index + 1}: ${line.trim().take(160)}"
                    }
                }
            }
        if (matches.isEmpty()) return "no matches for: $pattern"
        return matches.joinToString("\n") + if (matches.size >= MAX_MATCHES) "\n... truncated at $MAX_MATCHES matches" else ""
    }

    private fun read(relative: String, startLine: Int): String {
        val file = resolve(relative) ?: return "ERROR: path escapes the project root: $relative"
        if (!Files.isRegularFile(file)) return "ERROR: no such file: $relative"
        val lines = Files.readAllLines(file)
        val from = (startLine - 1).coerceIn(0, lines.size)
        val page = lines.drop(from).take(MAX_READ_LINES)
        val header = "$relative lines ${from + 1}..${from + page.size} of ${lines.size}\n"
        return (header + page.joinToString("\n")).take(MAX_CHARS)
    }

    private fun list(relative: String): String {
        val dir = resolve(relative.ifBlank { "." }) ?: return "ERROR: path escapes the project root: $relative"
        if (!dir.isDirectory()) return "ERROR: not a directory: $relative"
        val files = Files.walk(dir).asSequence().filter { Files.isRegularFile(it) }
            .map { it.relativeTo(sourceRoot).toString() }.sorted().take(200).toList()
        return files.joinToString("\n") + if (files.size >= 200) "\n... truncated" else ""
    }

    companion object {
        const val MAX_MATCHES = 50
        const val MAX_READ_LINES = 200
        const val MAX_CHARS = 12000
    }
}
