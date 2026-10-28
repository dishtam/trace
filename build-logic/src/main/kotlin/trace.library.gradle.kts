plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
}
kotlin { jvmToolchain(21) }
tasks.withType<Test> { useJUnitPlatform() }
