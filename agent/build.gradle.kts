plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
tasks.withType<JavaCompile> { options.release = 17 } // min target JDK (ADR-0011: 17/21/25)

dependencies {
    implementation(libs.bytebuddy)
    implementation(libs.bytebuddy.agent)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.shadowJar)
    systemProperty("trace.agentJar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
}

// ADR-0013: self-contained agent jar, Byte Buddy relocated so it can never clash
// with an app's own Byte Buddy/ASM (Spring/Hibernate ship both).
tasks.shadowJar {
    archiveClassifier = ""
    relocate("net.bytebuddy", "trace.shaded.bytebuddy")
    manifest {
        attributes(
            "Premain-Class" to "trace.agent.TraceAgent",
            "Agent-Class" to "trace.agent.TraceAgent",
            "Can-Retransform-Classes" to "true",
            "Can-Redefine-Classes" to "true",
        )
    }
}
tasks.jar { enabled = false }
tasks.named("build") { dependsOn(tasks.shadowJar) }
