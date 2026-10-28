package trace.graph

import java.nio.file.Files
import java.nio.file.Path

data class SourceSnippet(val className: String, val methodName: String?, val file: String,
                         val line: Int, val snippet: String, val truncated: Boolean)

/**
 * get_source backing index (architecture.md: static index lives in graph/).
 * Looks classes up ONLY under the configured source roots; no path from a
 * caller ever reaches the filesystem directly (mcp-tool-design rule 2).
 */
class SourceIndex(private val roots: List<Path>) {

    fun find(className: String, methodName: String?): SourceSnippet? {
        val relative = className.replace('.', '/')
        for (root in roots) {
            val normalizedRoot = root.toAbsolutePath().normalize()
            for (ext in listOf("java", "kt")) {
                val file = root.resolve("$relative.$ext").toAbsolutePath().normalize()
                // Fail closed: a crafted class name must never escape the source roots.
                if (!file.startsWith(normalizedRoot)) return null
                if (Files.isRegularFile(file)) return snippet(file, className, methodName)
            }
            // Inner classes / multiple classes per file: fall back to the outer class file.
            val outer = relative.substringBefore('$')
            if (outer != relative) for (ext in listOf("java", "kt")) {
                val file = root.resolve("$outer.$ext")
                if (Files.isRegularFile(file)) return snippet(file, className, methodName)
            }
        }
        return null
    }

    private fun snippet(file: Path, className: String, methodName: String?): SourceSnippet {
        val lines = Files.readAllLines(file)
        var start = 0
        if (methodName != null) {
            val idx = lines.indexOfFirst { it.contains(methodName) && it.contains('(') }
            if (idx >= 0) start = idx
        }
        val page = lines.drop(start).take(MAX_LINES)
        return SourceSnippet(className, methodName, file.fileName.toString(), start + 1,
            page.joinToString("\n"), truncated = lines.size - start > MAX_LINES)
    }

    companion object { const val MAX_LINES = 120 } // llm-strategy.md Proposed cap
}
