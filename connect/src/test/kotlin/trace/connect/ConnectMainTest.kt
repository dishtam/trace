package trace.connect

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.graph.TraceQueries
import trace.store.SqliteTraceStore
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import javax.tools.ToolProvider

class ConnectMainTest {

    @Test fun optionsRequireExactlyOneMode() {
        assertThrows(IllegalArgumentException::class.java) {
            parseOptions(arrayOf("--project", "x"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseOptions(arrayOf("--project", "x", "--test", "A", "--jar", "a.jar"))
        }
        val ok = parseOptions(arrayOf("--project", "x", "--jar", "a.jar", "--", "arg1"))
        assertEquals(RunMode.Jar(Path.of("a.jar")), ok.mode)
        assertEquals(listOf("arg1"), ok.appArgs)
    }

    @Test fun detectAllowPicksDominantPackagePrefix(@TempDir dir: Path) {
        val root = dir.resolve("src/main/java/com/example/app")
        Files.createDirectories(root)
        Files.writeString(root.resolve("A.java"), "package com.example.app;\nclass A {}")
        Files.writeString(root.resolve("B.java"), "package com.example.app;\nclass B {}")
        val other = dir.resolve("src/main/java/org/other")
        Files.createDirectories(other)
        Files.writeString(other.resolve("C.java"), "package org.other;\nclass C {}")
        assertEquals("com.example", ProjectFiles.detectAllow(dir))
        assertEquals(listOf(dir.resolve("src/main/java")), ProjectFiles.sourceRoots(dir))
    }

    @Test fun gradlePlanUsesInitScriptInjection(@TempDir dir: Path) {
        Files.writeString(dir.resolve("build.gradle"), "")
        val opts = ConnectOptions(dir, RunMode.Test("com.example.OrderTest"), "com.example",
            null, 1024, false, null, emptyList())
        val plan = planRun(opts, Path.of("agent.jar"), dir.resolve("events.jsonl"), dir.resolve("init.gradle"))
        val joined = plan.command.joinToString(" ")
        assertTrue(joined.contains("test"), joined)
        assertTrue(joined.contains("--tests"), joined)
        assertTrue(joined.contains("com.example.OrderTest"), joined)
        assertTrue(gradleInitScript("ARG").contains("jvmArgs 'ARG'"))
        assertTrue(gradleInitScript("""C:\repo\agent.jar=allow=com.acme;out=C:\x\events.jsonl""")
            .contains("C:/repo/agent.jar=allow=com.acme;out=C:/x/events.jsonl"),
            "init script must forward-slash Windows paths")
        assertTrue(agentArg(Path.of("agent.jar"), "com.example", dir.resolve("events.jsonl"), 1024)
            .startsWith("-javaagent:"))
    }

    @Test fun jarModeCapturesExternalRunEndToEnd(@TempDir dir: Path) {
        val agentJar = findAgentJar(Path.of(".."))
        org.junit.jupiter.api.Assumptions.assumeTrue(agentJar != null,
            "agent jar not built; run gradlew.bat :agent:shadowJar")
        val project = dir.resolve("external-app")
        val src = project.resolve("src/testapp")
        Files.createDirectories(src)
        Files.writeString(src.resolve("Main.java"), """
            package testapp;
            public class Main { public static void main(String[] args) { Helper.fail(); } }
        """.trimIndent())
        Files.writeString(src.resolve("Helper.java"), """
            package testapp;
            class Helper { static void fail() { throw new IllegalStateException("boom-external"); } }
        """.trimIndent())
        val classes = dir.resolve("classes"); Files.createDirectories(classes)
        val compiler = ToolProvider.getSystemJavaCompiler()
        assertEquals(0, compiler.run(null, null, null,
            "-d", classes.toString(), src.resolve("Main.java").toString(), src.resolve("Helper.java").toString()))
        val jar = project.resolve("app.jar")
        val manifest = Manifest().apply { mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0" }
        manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = "testapp.Main"
        JarOutputStream(Files.newOutputStream(jar), manifest).use { out ->
            Files.walk(classes).use { stream ->
                stream.filter { Files.isRegularFile(it) }.forEach { file ->
                    out.putNextEntry(java.util.jar.JarEntry(classes.relativize(file).toString().replace('\\', '/')))
                    Files.copy(file, out); out.closeEntry()
                }
            }
        }
        val outDir = dir.resolve("out")
        main(arrayOf("--project", project.toString(), "--jar", jar.toString(),
            "--allow", "testapp", "--agent", agentJar!!.toString(), "--out", outDir.toString()))
        val db = outDir.resolve("trace.db")
        assertTrue(Files.isRegularFile(db), "trace.db not created in $outDir")
        val runId = SqliteTraceStore(db.toString()).use { store ->
            (store.jdbcQuery("SELECT run_id FROM runs").single()["run_id"] as String)
        }
        val failure = TraceQueries(db.toString()).failurePoint(runId)
        assertNotNull(failure)
        assertEquals("java.lang.IllegalStateException", failure!!.exceptionType)
        assertEquals("testapp.Helper", failure.className)
    }
}
