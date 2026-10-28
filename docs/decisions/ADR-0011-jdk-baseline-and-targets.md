# ADR-0011: JDK baseline (tool) and target JDKs (agent)

## Context
Tool JDK must be an LTS the team actually has. Verified 2026-10-28: JDK 25 is the current LTS (Sep 2025, Oracle premier support to Sep 2030), JDK 21 LTS is supported to Sep 2028, Spring Boot 4.1 requires Java 17+; local dev machine runs JDK 21.0.10 LTS (`java -version`). Architecture Proposed targets 11/17/21/25 and asked whether Java 8 instrumentation is in scope.

## Decision
- **Tool builds/runs on JDK 21** (Gradle toolchain 21).
- **Phase 0 agent verified on JDK 21 first** (demo-app JDK). CI matrix then covers **17, 21, 25**. JDK 11 dropped from the Phase 0 matrix (revisit if a real user needs it).
- **Java 8 is NOT an instrumentation target.** A JDK 8 toolchain exists only to compile/run Defects4J corpus projects (evaluation.md).

## Consequences
- (+) No local installs needed; widest Spring Boot 4.x compatibility.
- (−) Apps on JDK 8/11 cannot be instrumented in v1 — stated limit.
- Sources: [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html), [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html).

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
