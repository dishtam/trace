# Tech Stack — Verified Status

Verified by web search 2026-10-27. Search snippets are discovery-grade:
entries marked TODO(verify) must be confirmed on the project's own release
page before pinning. License target: Apache-2.0 project (ADR-0004).

| Dependency | Purpose here | Version / last release (as found) | License | Maintenance | Source |
|---|---|---|---|---|---|
| Byte Buddy | Agent bytecode transformation | **1.18.16** (verified Maven Central 2026-10-28) | Apache-2.0 | Active | [releases](https://github.com/raphw/byte-buddy/releases), [bytebuddy.net](https://bytebuddy.net/) |
| OW2 ASM | Low-level bytecode (under Byte Buddy; `Analyzer` for local flow) | 9.11.1 noted 17 Oct 2026 on asm.ow2.io (TODO(verify)) | BSD-3-Clause | Active | [asm.ow2.io](http://asm.ow2.io) |
| JDI (`jdk.jdi`) | On-demand deep dive, `confirm_hypothesis` | Part of the JDK (JPDA) — no separate dep | JDK license (GPLv2+CE for OpenJDK) | JDK-maintained | [Oracle JPDA](https://docs.oracle.com/en/java/javase/11/docs/specs/jpda/jpda.html) |
| SQLite JDBC (xerial `org.xerial:sqlite-jdbc`) | Spike store driver (ADR-0008) | **3.53.4.0** (verified Maven Central 2026-10-28) | Apache-2.0 | Active | [xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) |
| DuckDB JDBC (`org.duckdb:duckdb_jdbc`) | Storage comparison candidate (ADR-0008) | **2.0.0.0** (verified Maven Central 2026-10-28, S0-8) | MIT (TODO(verify)) | Active | [duckdb](https://github.com/duckdb/duckdb) |
| MCP Kotlin SDK / MCP Java SDK | `mcp-server/` implementation | **Kotlin SDK 0.16.0 pinned** (verified Maven Central 2026-10-28; see Pinned table below). ADR-0005 = Kotlin → Kotlin SDK chosen | TODO(verify) license file at pin | Active | [MCP SDKs](https://deepwiki.com/modelcontextprotocol/modelcontextprotocol/3-sdk-implementations), [Spring blog](https://Spring.io/blog/2025/02/14/mcp-java-sdk-released-2) |
| Anthropic Java SDK | LLM client (if not raw HTTP) | TODO(verify): does an official Anthropic Java SDK exist at Phase 0 start, version + license? | TODO(verify) | TODO(verify) | Start from [anthropic.com](https://www.anthropic.com/news/claude-opus-4-8) — no SDK fact verified in this pass |
| Meta Model API client (Muse Wonder / Muse Spark) | Second LLM provider (ADR-0010) | TODO(verify): official Java/Kotlin SDK? Muse Code docs mention TypeScript (preview) and Python SDKs for its session protocol, not a JVM model-API SDK — likely raw HTTP client | TODO(verify) | Active (launched 2026) | [Muse Wonder model page](https://dev.meta.ai/models/muse-wonder) |
| JavaParser | Static source index (`get_source`, symbol mapping) | 3.28.2 (from javaparser GitHub README) | LGPL-3 / Apache-2.0 dual (TODO(verify) exact terms — historically dual-licensed; must confirm Apache option before use) | Active | [javaparser/javaparser](https://github.com/javaparser/javaparser) |
| Spoon (INRIA) | Alternative static analysis/transformation | 11.x line (11.2.1 found); requires Java 11+ | TODO(verify): CeCILL-C/MIT dual historically — confirm | Active | [Spoon](https://github.com/INRIA/spoon) |
| tree-sitter + Java bindings | Multi-language parsing groundwork (post-Java) | TODO(verify): binding maturity for Kotlin/JVM use | MIT (core, TODO(verify)) | Active | TODO(verify) at adoption |
| Cytoscape.js | Optional human graph UI (post-v1) | TODO(verify) at UI start | MIT (TODO(verify)) | Active | [cytoscape.js](https://github.com/cytoscape/cytoscape.js) |
| React Flow (`@xyflow/react`) | Alternative UI (node-based) | v12 line (`@xyflow/react`, 12.4.x found) | MIT (TODO(verify)) | Active | [React Flow 12](https://xyflow.com/blog/react-flow-12-release) |
| elkjs | Graph layout for UI | 0.12.0 (Jul 2026, secondary source — TODO(verify)) | EPL-2.0 (TODO(verify) — **flag:** Eclipse license needs ADR-0004 compatibility check before adoption) | Active | [elkjs](https://github.com/kieler/elkjs) |
| JUnit 5 | Tests across modules | **5.13.4** (verified Maven Central 2026-10-28) | EPL-2.0 — **flag:** same compatibility check as elkjs (test-only scope lowers risk, still record it) | Active | [junit5](https://github.com/junit-team/junit5) |
| JMH | Overhead microbenchmarks | TODO(verify) pin | GPLv2+CE (JDK project) — **flag:** benchmark-only; confirm no distribution impact | Active | [OpenJDK JMH](https://github.com/openjdk/jmh) |

## Embedded graph DB — separately verified

| Candidate | Status found | Verdict |
|---|---|---|
| Kuzu (KuzuDB) | **Upstream repo archived Oct 2025** after the company was acquired (Apple, per reporting); last release 0.11.3; community forks (e.g. Ladybug) without established maintenance | ❌ Do not adopt (ADR-0008). Sources: [Kuzu archived analysis](https://fr.zeststream.ai/briefs/frankengraphdb), [FreshPorts deprecation](https://www.freshports.org/databases/kuzu/), [Ladybug fork](https://thedataquarry.com/blog/from-kuzu-to-ladybug/) |
| Neo4j (embedded/server) | TODO(verify) if ever revisited | Not adopted: operational weight for a local tool; GPLv3 community edition would also conflict-flag under ADR-0004 — TODO(verify) |

## Flags summary

- ⚠️ **License-check before adoption:** JavaParser dual-license terms, Spoon license, elkjs + JUnit 5 (EPL-2.0), JMH (GPL+CE, benchmark-only). None pinned until checked — TODO(verify) each at Phase 0 build creation.
- ⚠️ **Unverified in this pass:** Anthropic Java SDK existence/version; Meta Model API JVM client/SDK; MCP SDK versions; tree-sitter JVM binding; exact latest patches for Byte Buddy/ASM/SQLite/DuckDB. All marked TODO(verify) above, not guessed.
- ❌ **Unmaintained/archived:** Kuzu — excluded.

## Pinned at Week 1 build creation (2026-10-28, Maven Central verified)

| Dependency | Version | Note |
|---|---|---|
| Gradle (wrapper) | 9.6.0 | committed wrapper, distributionUrl in gradle/wrapper/gradle-wrapper.properties |
| Kotlin | 2.3.21 | bundled with Gradle 9.6.0 (`gradle -v`) |
| Spring Boot (demo-app) | 4.1.2 | latest stable on Maven Central; 4.2.x is RC only. Boot 4 needs spring-boot-starter-webmvc-test for MockMvc |
| GradleUp Shadow | 9.6.1 | agent jar relocation (ADR-0013) |
| H2 | 2.3.232 | demo-app in-memory DB only |




| MCP Kotlin SDK (mcp-server) | 0.16.0 | Verified Maven Central 2026-10-28; pulls kotlinx-serialization-json 1.11.0 + kotlin-stdlib 2.4.21 |
| kotlinx-serialization-json / coroutines | 1.11.0 / 1.10.2 | mcp-server tool JSON + stdio main |


