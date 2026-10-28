plugins {
    id("trace.library")
    application
}
application { mainClass = "trace.eval.EvalMainKt" }
dependencies {
    implementation(project(":mcp-server"))
    implementation(project(":store"))
    implementation(libs.duckdb.jdbc)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.io)
    implementation(project(":collector"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    testImplementation(project(":store"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
