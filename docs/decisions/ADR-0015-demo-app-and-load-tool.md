# ADR-0015: Demo app stack and load tool

## Context
Demo-app-spec needs a pinned Spring Boot + a load script for the S0-4 overhead benchmark. Verified 2026-10-28: Spring Boot 4.1.2 is the latest stable (docs.spring.io), requires Java 17+, compatible to Java 27; 3.5.x remains maintained ([Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)). Load options: k6 (extra external install), JMH (microbenchmark focus, poor fit for HTTP request mixes), or a small in-repo Kotlin HTTP client.

## Decision
- **Demo app: Spring Boot 4.1.x on JDK 21** (exact patch TODO(verify) on Maven Central at S0-1; recorded in stack.md).
- **Load tool: a small Kotlin HTTP client** (`demo-app` load main / script) issuing the spec's repeatable request mix. No k6/JMH dependency in Week 1.

## Consequences
- (+) Zero external installs on Windows; load behaviour is code we control and can make deterministic.
- (−) Not a realistic production load generator — acceptable: S0-4 measures relative overhead (agent on/off), not absolute throughput.

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
