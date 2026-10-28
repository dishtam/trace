# ADR-0012: Gradle wrapper, version catalog, convention plugins

## Context
8 Gradle modules need shared Kotlin/JDK/test config without copy-paste. Verified: Gradle 9.0 (2025-07-31) requires JVM 17+ and uses Kotlin 2 ([Gradle 9.0.0 release notes](https://docs.gradle.org/9.0.0/release-notes.html)); exact latest 9.x patch is TODO(verify) at wrapper generation (`gradle wrapper --gradle-version …` output is the record).

## Decision
- **Gradle 9.x via the committed wrapper** (`gradlew` + `gradlew.bat` both generated and committed; distribution checksum pinned).
- **Version catalog** `gradle/libs.versions.toml` is the only place dependency/plugin versions live.
- **`build-logic/` convention plugins** hold shared Kotlin, toolchain (ADR-0011), and test config. Modules declare only their differences.

## Consequences
- (+) One version bump point; dependency-rule checks can live in build-logic.
- (−) build-logic is code to maintain; debugging convention plugins is a known cost.
- Exact Gradle patch + every dependency version: verified on Maven Central / official release pages when added, recorded in docs/stack.md in the same change (never invented).

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
