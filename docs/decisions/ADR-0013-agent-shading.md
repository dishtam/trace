# ADR-0013: Agent jar shading — Shadow with full relocation

## Context
Target apps (Spring/Hibernate) ship their own Byte Buddy and ASM. An agent jar carrying unrelocated copies — especially on/near the bootstrap classpath — can shadow or clash with the app's copies. Options: no shading (spike-fast, known clash risk), relocate Byte Buddy only, relocate all agent dependencies. The GradleUp Shadow plugin (`com.gradleup.shadow`) is the maintained Shadow line for Gradle 9; the legacy `com.github.johnrengelman.shadow` does not support Gradle 9 (TODO(verify): exact Shadow version at build time, recorded in stack.md).

## Decision
**Shadow `shadowJar` for `agent/`, relocating Byte Buddy and ASM** (and any future agent dependency) under a `trace.shaded.*` prefix. The agent jar is self-contained; agent code references only relocated packages.

## Consequences
- (+) No classpath clash with app-bundled Byte Buddy/ASM; one-file `-javaagent` artefact.
- (−) Stack traces inside the agent show relocated names; debugging the agent needs the mapping kept in mind.
- Relocation completeness is tested (jar content check: no unrelocated `net.bytebuddy`/`org.objectweb` entries).

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
