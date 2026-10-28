plugins { base }

// Architecture guard (docs/architecture.md): no cycles; agent depends on nothing;
// graph->store; mcp-server->graph,jdi-driver; others follow module order.
val forbiddenDeps = mapOf(
    "agent" to setOf("collector", "store", "graph", "jdi-driver", "mcp-server", "eval", "demo-app"),
    "collector" to setOf("graph", "jdi-driver", "mcp-server"),
    "store" to setOf("collector", "graph", "jdi-driver", "mcp-server"),
    "graph" to setOf("mcp-server", "jdi-driver"),
    "demo-app" to setOf("agent", "collector", "store", "graph", "jdi-driver", "mcp-server", "eval")
)
tasks.register("checkArchitecture") {
    group = "verification"
    description = "Fails on forbidden inter-module dependencies (architecture.md)."
    doLast {
        subprojects.forEach { p ->
            val bad = p.configurations.flatMap { c -> c.dependencies }
                .filterIsInstance<ProjectDependency>()
                .map { it.path.removePrefix(":") }
                .intersect(forbiddenDeps[p.name].orEmpty())
            require(bad.isEmpty()) { "Forbidden dependencies in ${p.name}: $bad" }
        }
        println("checkArchitecture: OK")
    }
}
tasks.named("check") { dependsOn("checkArchitecture") }
subprojects { tasks.withType<AbstractArchiveTask> { isPreserveFileTimestamps = false; isReproducibleFileOrder = true } }
