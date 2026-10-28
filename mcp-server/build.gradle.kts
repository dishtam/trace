plugins {
    id("trace.library")
    application
}
application { mainClass = "trace.mcpserver.McpServerKt" }
dependencies {
    implementation(project(":graph"))
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    // Logs must go to stderr: stdout is the MCP protocol channel.
    runtimeOnly(libs.slf4j.simple)
    testImplementation(project(":store"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test {
    // Real test runtime classpath for the stdio subprocess test (java.class.path
    // inside a Gradle test worker is only the worker bootstrap).
    doFirst { systemProperty("mcp.test.classpath", classpath.asPath) }
}
