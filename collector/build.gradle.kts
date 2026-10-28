plugins {
    id("trace.library")
    application
}
application { mainClass = "trace.collector.TraceIngest" }
dependencies { implementation(project(":store")); testImplementation(platform(libs.junit.bom)); testImplementation(libs.junit.jupiter); testRuntimeOnly("org.junit.platform:junit-platform-launcher") }
