plugins {
    id("trace.library")
    application
}
application { mainClass = "trace.connect.ConnectMainKt" }
dependencies {
    implementation(project(":collector"))
    implementation(project(":mcp-server"))
    implementation(project(":graph"))
    testImplementation(project(":store"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
