# Muse Code Setup (building TRACE)

TRACE is built with Meta's **Muse Code** coding agent. This page is for the
*build* tool. The LLM *inside* TRACE is a separate, pluggable choice
([ADR-0010](decisions/ADR-0010-pluggable-llm-provider.md)).

Source: Muse Code docs — [configuration](https://dev.meta.ai/docs/muse-code/configuration),
[extending](https://dev.meta.ai/docs/muse-code/extending) — read 2026-10-27.
Re-check them when Muse Code updates; flags and paths below may change.

## How Muse Code reads this repo

| What | Where in this repo | Notes |
|---|---|---|
| Project instructions | `AGENTS.md` (root) | Per directory, Muse loads the first of `AGENTS.md`, `CLAUDE.md`, `.agents/AGENTS.md`, `.claude/CLAUDE.md`. Keep only `AGENTS.md`. |
| Project skills | `.agents/skills/<id>/SKILL.md` (7 skills) | Muse also scans `.claude/skills/` and `.codex/skills/` — keep those absent to avoid duplicate IDs. |
| Project hooks | `.muse/hooks.json` | Not created yet (see Planned hooks). |
| Project memory | `.agents/memory/MEMORY.md` | Not used yet. Loads **even in untrusted workspaces** — treat it as a prompt-injection surface; review changes to it. |
| MCP servers | your `~/.config/muse/settings.json` → `mcp_servers` | User-level, not in the repo. |

Project instructions, skills and hooks load only after you **trust the workspace**.

## One-time setup

1. Open the repo in Muse Code and trust the workspace.
2. Run `muse skills list` — confirm each of these appears **exactly once**, from the project source:
   `add-bug-fixture`, `adr`, `debug-investigation`, `eval-localization`, `instrumentation-safety`, `mcp-tool-design`, `trace-event-schema`.
3. **Personal skills from other agents:** Muse also loads `~/.claude/skills` and `~/.codex/skills` by default. If Claude/Codex-specific skills show up and add noise, disable the foreign personal skill roots (a user preference — TODO(verify) exact setting name in Muse docs).
4. **Contributor mode: keep it off for this repo** (ADR-0006). Cheaper Contributor plans/models let Meta train on submitted data, which would include our code and any captured values pasted into a session. Check your saved setting; `--contributor-mode off` forces it per run.
5. Review what Muse imports from your existing Claude Code / Codex setup (`/migrate`, `muse skills import`) before running it. Only import what this project needs.
6. Don't run `muse init --force` — it overwrites `AGENTS.md`. `muse init --dry-run` is safe.

## Model and effort

- Muse Code's default model is `muse-spark-1.2`. Switch with `--model <id>` or `/models`. Use **Muse Wonder** for design-heavy work. TODO(verify): exact Muse Wonder model ID — check `/models`.
- Reasoning effort default is `high`. Use `xhigh` for trace-schema, ADR, and instrumentation design; `medium` for routine doc/test edits. Change mid-session with `/effort`.
- The four background observer agents (memory recall, skill recall, goal tracking, verification) are on by default and add token usage. Keep **skill recall** and **verification** on — they push the agent toward the mandatory skills and toward checking its claims.

## Recommended workflow

| Situation | Use |
|---|---|
| Open `TODO(decide)` items before a phase | `/grill` on the doc (trace-schema.md, spike-acceptance.md, evaluation.md), then record outcomes with the `adr` skill |
| Each roadmap task | `/plan`, approve, then build |
| Independent modules in parallel (e.g. `store/` and `demo-app/`) | Subagents with **worktree isolation** for every writer; read-only research children can share the checkout |
| Evaluation runs in CI | `muse exec --prompt-file <task> --max-model-steps <n> --json`; gate on the eval/test command's result, not `muse exec`'s exit code. CI sandbox on Linux needs bubblewrap (non-musl). |
| Resuming a long run headless | `muse exec --session-id <uuid> "Continue the task."` |

## User settings snippet

`~/.config/muse/settings.json` must contain `"schema_version": 1`, or every command
fails at startup. Example, once `mcp-server/` exists (not before):

```json
{
  "schema_version": 1,
  "mcp_servers": {
    "trace": {
      "transport": "stdio",
      "command": "TODO(decide): launch command for mcp-server",
      "args": [],
      "mode": "optional"
    }
  }
}
```

`mode: "optional"` matters: a `required` server that fails to start aborts the
whole Muse run. MCP servers are not sandboxed — only connect ones you trust.

## Planned hooks (create `.muse/hooks.json` once the Gradle build exists)

| Event | Matcher | Action |
|---|---|---|
| `PostToolUse` | file edits under `agent/` | run agent unit tests; surface failures to the agent |
| `PostToolUse` | file edits under `agent/` | remind: `instrumentation-safety` skill + PR-CHECKLIST are mandatory |
| `PostToolUse` | edits to `docs/trace-schema.md` | remind: `trace-event-schema` skill (version bump, DDL, tests) |

TODO(verify): `.muse/hooks.json` file schema and matcher syntax — question: confirm
on the Muse Code hooks docs before writing it. Hooks run **outside** Muse's
sandbox through your shell, so review every command before adding it.
