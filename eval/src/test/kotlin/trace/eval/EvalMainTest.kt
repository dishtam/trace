package trace.eval

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class EvalMainTest {
    @Test fun repoRootFoundFromModuleDir(@TempDir dir: Path) {
        // Layout: <root>/eval/fixtures, <root>/demo-app; run starts in <root>/eval.
        val root = dir.resolve("repo")
        Files.createDirectories(root.resolve("eval/fixtures"))
        Files.createDirectories(root.resolve("demo-app"))
        assertEquals(root, findRepoRoot(root.resolve("eval")))
        assertEquals(root, findRepoRoot(root))
    }

    @Test fun repoRootUnchangedWhenNoMarkerDirs(@TempDir dir: Path) {
        val start = dir.resolve("nowhere")
        Files.createDirectories(start)
        assertEquals(start, findRepoRoot(start))
    }
}
