package trace.connect

import trace.collector.TraceIngest
import trace.graph.TraceQueries
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

/**
 * trace connect: attach the TRACE agent to an external Java codebase, record
 * one failing run, ingest it, and hand over a ready MCP server configuration.
 *
 *   gradlew.bat :connect:run --args="--project C:/code/myapp --test com.example.OrderTest --allow com.example"
 *   gradlew.bat :connect:run --args="--project C:/code/myapp --jar build/libs/app.jar"
 *   gradlew.bat :connect:run --args="--project C:/code/myapp --command \"mvn -Dtest=OrderTest test\""
 *
 * Modes: --test (Gradle/Maven auto-detected), --jar, --command (any shell
 * command; the agent is injected via JAVA_TOOL_OPTIONS). --allow is the
 * package allow-list for capture; when omitted it is inferred from the
 * project's package declarations. Artefacts land in <project>/.trace/
 * (override with --out). The target's own exit code never fails connect:
 * a failing test is the expected input. --serve then launches the MCP
 * server (stdio) over the recorded database.
 */
sealed interface RunMode {
    data class Test(val selector: String) : RunMode
    data class Jar(val jar: Path) : RunMode
    data class Command(val command: String) : RunMode
}

data class ConnectOptions(
    val project: Path,
    val mode: RunMode,
    val allow: String?,
    val out: Path?,
    val capacity: Int,
    val serve: Boolean,
    val agentJar: Path?,
    val appArgs: List<String>,
)

data class PlannedRun(val command: List<String>, val env: Map<String, String>, val description: String)

private val IS_WINDOWS = System.getProperty("os.name").lowercase().contains("win")

fun agentArg(agentJar: Path, allow: String, events: Path, capacity: Int): String =
    "-javaagent:${agentJar.absolutePathString()}=allow=$allow;out=${events.absolutePathString()};capacity=$capacity"

/**
 * Gradle init script: adds the agent to every Test task without touching the
 * target's build files. Paths are forward-slashed: Windows backslashes are
 * Groovy string escapes and break script compilation (found in E2E, 2026-11-01).
 */
fun gradleInitScript(agentArgValue: String): String {
    val safe = agentArgValue.replace('\\', '/').replace("'", "")
    return """
        allprojects {
            tasks.withType(Test).configureEach {
                jvmArgs '$safe'
            }
        }
    """.trimIndent()
}

fun planRun(options: ConnectOptions, agentJarPath: Path, events: Path, initScript: Path?, traceRoot: Path? = null): PlannedRun {
    val arg = agentArg(agentJarPath, options.allow ?: "", events, options.capacity)
    return when (val mode = options.mode) {
        is RunMode.Jar -> {
            val java = Path.of(System.getProperty("java.home"), "bin", if (IS_WINDOWS) "java.exe" else "java").absolutePathString()
            PlannedRun(listOf(java, arg, "-jar", mode.jar.absolutePathString()) + options.appArgs,
                emptyMap(), "java -jar ${mode.jar.fileName}")
        }
        is RunMode.Command -> {
            val existing = System.getenv("JAVA_TOOL_OPTIONS")
            val toolOptions = listOfNotNull(existing?.takeIf { it.isNotBlank() }, arg).joinToString(" ")
            val shell = if (IS_WINDOWS) listOf("cmd", "/c", mode.command) else listOf("sh", "-c", mode.command)
            PlannedRun(shell, mapOf("JAVA_TOOL_OPTIONS" to toolOptions), "shell command with JAVA_TOOL_OPTIONS agent injection")
        }
        is RunMode.Test -> when (ProjectFiles.buildSystem(options.project)) {
            BuildSystem.GRADLE -> {
                val wrapperName = if (IS_WINDOWS) "gradlew.bat" else "gradlew"
                val wrapper = options.project.resolve(wrapperName)
                // Fall back to the TRACE repo's wrapper for projects that have none.
                val traceWrapper = traceRoot?.resolve(wrapperName)
                val base = when {
                    Files.isRegularFile(wrapper) -> wrapper.absolutePathString()
                    traceWrapper != null && Files.isRegularFile(traceWrapper) -> traceWrapper.absolutePathString()
                    else -> "gradle"
                }
                val cmd = listOf(base, "-I", initScript!!.absolutePathString(), "test", "--tests", mode.selector)
                PlannedRun(if (IS_WINDOWS) listOf("cmd", "/c") + cmd else cmd,
                    emptyMap(), "Gradle test ${mode.selector} (init-script agent injection)")
            }
            BuildSystem.MAVEN -> {
                val existing = System.getenv("JAVA_TOOL_OPTIONS")
                val toolOptions = listOfNotNull(existing?.takeIf { it.isNotBlank() }, arg).joinToString(" ")
                // Prefer the project's Maven wrapper when present (mvnw/mvnw.cmd).
                val wrapper = options.project.resolve(if (IS_WINDOWS) "mvnw.cmd" else "mvnw")
                val mvn = if (Files.isRegularFile(wrapper)) wrapper.absolutePathString() else "mvn"
                val cmd = listOf(mvn, "-Dtest=${mode.selector}", "test")
                PlannedRun(if (IS_WINDOWS) listOf("cmd", "/c") + cmd else cmd,
                    mapOf("JAVA_TOOL_OPTIONS" to toolOptions), "Maven test ${mode.selector} (JAVA_TOOL_OPTIONS agent injection)")
            }
            BuildSystem.NONE -> throw IllegalArgumentException(
                "no Gradle wrapper/build file or pom.xml in ${options.project}; use --jar or --command for this project")
        }
    }
}

internal fun findAgentJar(start: Path): Path? {
    var current: Path? = start.toAbsolutePath().normalize()
    while (current != null) {
        val candidate = current.resolve("agent/build/libs/agent.jar")
        if (Files.isRegularFile(candidate)) return candidate
        current = current.parent
    }
    return null
}

internal fun findTraceRoot(start: Path): Path? {
    var current: Path? = start.toAbsolutePath().normalize()
    while (current != null) {
        if (Files.isDirectory(current.resolve("agent")) && Files.isRegularFile(current.resolve("settings.gradle.kts"))) return current
        current = current.parent
    }
    return null
}

fun main(args: Array<String>) {
    val options = parseOptions(args)
    val project = options.project.toAbsolutePath().normalize()
    require(Files.isDirectory(project)) { "project directory not found: $project" }

    val agentJarPath = options.agentJar ?: findAgentJar(Path.of("."))
        ?: fail("agent jar not found (looked upward from the working directory for agent/build/libs/agent.jar). " +
            "Build it first: gradlew.bat :agent:shadowJar, or pass --agent <path>")
    val allow = options.allow ?: ProjectFiles.detectAllow(project)
        ?: fail("could not infer an allow-list package from $project sources; pass --allow <package.prefix> explicitly")

    val outDir = (options.out ?: project.resolve(".trace")).toAbsolutePath().normalize()
    Files.createDirectories(outDir)
    val events = outDir.resolve("events.jsonl")
    val db = outDir.resolve("trace.db")
    Files.deleteIfExists(events); Files.deleteIfExists(db)

    var initScript: Path? = null
    if (options.mode is RunMode.Test && ProjectFiles.buildSystem(project) == BuildSystem.GRADLE) {
        initScript = outDir.resolve("trace-connect.init.gradle")
        Files.writeString(initScript, gradleInitScript(agentArg(agentJarPath, allow, events, options.capacity)))
    }
    val plan = planRun(options.copy(allow = allow), agentJarPath, events, initScript, findTraceRoot(Path.of(".")))

    println("trace connect")
    println("  project : $project")
    println("  capture : allow=$allow (packages), capacity=${options.capacity}")
    println("  run     : ${plan.description}")
    println("  command : ${plan.command.joinToString(" ")}")
    val exit = ProcessBuilder(plan.command).directory(project.toFile())
        .redirectErrorStream(true).inheritIO()
        .apply { environment().putAll(plan.env) }
        .start().waitFor()
    println("  target exit code: $exit (a failing run is the expected input; continuing)")

    if (!Files.isRegularFile(events) || Files.size(events) == 0L) {
        fail("no trace events were captured in $events. Check that the run actually executed code under " +
            "the allow-listed packages ($allow), and that the test/run reached the failure. " +
            "Nothing was ingested.")
    }
    val summary = TraceIngest.ingest(events.toFile(), db.toFile(), app = project.fileName.toString())
    println("  ingested: run=${summary.runId} events=${summary.events} dropped=${summary.dropped} complete=${summary.complete}")
    println("  database: $db")

    val queries = TraceQueries(db.absolutePathString())
    val failure = queries.failurePoint(summary.runId)
    if (failure != null) {
        println("  failure : ${failure.exceptionType} at ${failure.className}#${failure.methodName} (event ${failure.eventId})")
    } else {
        println("  failure : no uncaught exception in this run (non-exception failure; start from get_call_subtree)")
    }

    val roots = ProjectFiles.sourceRoots(project)
    val traceRoot = findTraceRoot(Path.of("."))
    val serverBin = traceRoot?.resolve(
        if (IS_WINDOWS) "mcp-server/build/install/mcp-server/bin/mcp-server.bat" else "mcp-server/build/install/mcp-server/bin/mcp-server")
    println()
    println("Investigate it: register the TRACE MCP server in your client and ask it to investigate run ${summary.runId}.")
    if (serverBin != null && Files.isRegularFile(serverBin)) {
        println("MCP server command: ${serverBin.absolutePathString()} ${db.absolutePathString()} ${roots.joinToString(" ") { it.absolutePathString() }}")
    } else {
        println("Build the server launcher once: gradlew.bat :mcp-server:installDist")
        println("Then run: mcp-server/build/install/mcp-server/bin/mcp-server${if (IS_WINDOWS) ".bat" else ""} ${db.absolutePathString()} ${roots.joinToString(" ") { it.absolutePathString() }}")
    }
    println("Tools available to the investigator: get_failure_point, trace_back, get_call_subtree, get_source (root causes are labelled unconfirmed; no confirm_hypothesis in this build).")

    if (options.serve) {
        println("Serving MCP on stdio (Ctrl+C to stop)...")
        val serverCmd = mutableListOf(
            Path.of(System.getProperty("java.home"), "bin", if (IS_WINDOWS) "java.exe" else "java").absolutePathString(),
            "-cp", System.getProperty("java.class.path"), "trace.mcpserver.McpServerKt",
            db.absolutePathString())
        roots.forEach { serverCmd += it.absolutePathString() }
        val code = ProcessBuilder(serverCmd).inheritIO().start().waitFor()
        exitProcess(code)
    }
}

private fun fail(message: String): Nothing {
    System.err.println("trace connect: $message")
    exitProcess(2)
}

internal fun parseOptions(args: Array<String>): ConnectOptions {
    val values = mutableMapOf<String, String>()
    var serve = false
    val appArgs = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "--" -> { appArgs += args.drop(i + 1); break }
            arg == "--serve" -> { serve = true; i++ }
            arg.startsWith("--") && i + 1 < args.size -> { values[arg.removePrefix("--")] = args[i + 1]; i += 2 }
            else -> throw IllegalArgumentException("unrecognised argument: $arg (see the ConnectMain usage doc)")
        }
    }
    val modes = listOfNotNull(
        values["test"]?.let { RunMode.Test(it) },
        values["jar"]?.let { RunMode.Jar(Path.of(it)) },
        values["command"]?.let { RunMode.Command(it) },
    )
    require(modes.size == 1) { "exactly one run mode is required: --test <selector>, --jar <path>, or --command <cmd>" }
    return ConnectOptions(
        project = Path.of(values["project"] ?: throw IllegalArgumentException("--project <path> is required")),
        mode = modes.single(),
        allow = values["allow"],
        out = values["out"]?.let { Path.of(it) },
        capacity = values["capacity"]?.toInt() ?: 262144,
        serve = serve,
        agentJar = values["agent"]?.let { Path.of(it) },
        appArgs = appArgs,
    )
}
