package trace.eval

import kotlinx.serialization.json.JsonObject

data class InvestigationResult(
    val report: String,
    val claimedRootCause: String?,
    val budget: BudgetTracker,
    val toolCallsLog: List<String>,
    val wallMillis: Long,
    val budgetExceeded: Boolean,
)

/**
 * One investigation loop, identical for both runners (evaluation.md fairness):
 * same client, model, budget, prompt template. Only the tool set (and, for
 * TRACE, the run_id in the starting information) differs.
 */
class InvestigationRunner(
    private val client: LlmClient,
    private val tools: InvestigationTools,
    private val maxTurns: Int = 40,
    private val budgetCapUsd: Double = 0.50,
    private val maxToolCalls: Int = 50,
) {
    fun investigate(startingInfo: String): InvestigationResult {
        val started = System.nanoTime()
        val budget = BudgetTracker(client.model, budgetCapUsd, maxToolCalls)
        val defs = tools.defs()
        val messages = mutableListOf<Msg>(Msg.User(startingInfo))
        val log = mutableListOf<String>()
        var finalText: String? = null
        var exceeded = false

        for (turn in 0 until maxTurns) {
            val reply = client.complete(SYSTEM_PROMPT, messages, defs)
            budget.record(reply.usage, 0)
            when (reply) {
                is LlmReply.Text -> { finalText = reply.text; break }
                is LlmReply.Calls -> {
                    messages += Msg.AssistantCalls(reply.calls, reply.text)
                    val results = mutableListOf<Pair<String, String>>()
                    for (call in reply.calls) {
                        budget.record(Usage(0, 0), 1)
                        if (budget.exceeded()) exceeded = true
                        val result = if (exceeded) "not executed: investigation budget exhausted" else try {
                            tools.call(call.name, call.arguments)
                        } catch (e: Exception) {
                            "ERROR: tool ${call.name} failed: ${e.message}"
                        }
                        log += "${call.name}(${call.arguments}) -> ${result.take(120).replace("\n", " ")}"
                        results += call.id to result.take(MAX_TOOL_RESULT_CHARS)
                    }
                    // Every call above got a result entry, executed or not: ending
                    // with unanswered tool calls makes the next API request invalid.
                    messages += Msg.ToolResults(results)
                    if (budget.exceeded()) { exceeded = true; break }
                }
            }
        }
        // One concluding turn on budget/turn exhaustion (added after the external
        // databind comparison showed both runners dying mid-investigation with
        // nothing scored). It runs in a FRESH conversation with a compact
        // evidence digest: concluding inside the exhausted conversation failed
        // twice in practice (HTTP 400 on unanswered tool calls, then a literal
        // "null" reply from the bloated context). No tools are offered.
        if (finalText == null) {
            try {
                val digest = log.joinToString("\n").take(8000)
                val wrap = client.complete(SYSTEM_PROMPT, listOf(Msg.User(
                    startingInfo + "\n\nYour investigation budget is exhausted; this is a fresh concluding " +
                    "conversation. Below is the tool-call log of your investigation (each result truncated). " +
                    "Using only this evidence, write your final report now in the exact required shape, ending " +
                    "with the ROOT_CAUSE line naming your single best-supported class and method.\n\n" +
                    "Investigation log:\n" + digest)), emptyList())
                budget.record(wrap.usage, 0)
                log += "wrap-up reply: ${wrap::class.simpleName}"
                if (wrap is LlmReply.Text && wrap.text.isNotBlank() &&
                    !wrap.text.trim().equals("null", ignoreCase = true)) {
                    finalText = wrap.text
                } else {
                    // One retry, plain-text only: a first concluding reply of
                    // tool_calls/blank/"null" must not silently score zero.
                    val retry = client.complete(SYSTEM_PROMPT, listOf(Msg.User(
                        "Reply with plain text only; do not call tools. " +
                        "Write the final investigation report now, ending with exactly one line: " +
                        "ROOT_CAUSE: <fully-qualified-class>#<method>\n\n" +
                        "Symptom and investigation evidence:\n" + startingInfo.take(2000) + "\n" + digest)), emptyList())
                    budget.record(retry.usage, 0)
                    log += "wrap-up retry reply: ${retry::class.simpleName}"
                    if (retry is LlmReply.Text && retry.text.isNotBlank() &&
                        !retry.text.trim().equals("null", ignoreCase = true)) finalText = retry.text
                }
            } catch (e: Exception) {
                log += "wrap-up concluding call failed: ${e.message}"
            }
        }
        val report = finalText ?: "Investigation stopped before a final report (budget or turn limit). Partial evidence is in the tool log."
        return InvestigationResult(
            report = report,
            claimedRootCause = ROOT_CAUSE_REGEX.find(report)?.groupValues?.get(1)?.trim(),
            budget = budget, toolCallsLog = log,
            wallMillis = (System.nanoTime() - started) / 1_000_000,
            budgetExceeded = exceeded,
        )
    }

    companion object {
        const val MAX_TOOL_RESULT_CHARS = 12000
        val ROOT_CAUSE_REGEX = Regex("ROOT_CAUSE:\\s*(.+)")

        // Shared investigation prompt (debug-investigation protocol). The only
        // runner-specific text is the tool list the API carries and the run_id
        // line in the starting information — never the instructions.
        const val SYSTEM_PROMPT = """You are investigating a software failure in a Java Spring Boot service (an orders/inventory/pricing demo). Follow this protocol exactly:
1. Start from the failure evidence in the starting information. State in one line what failed and where.
2. Use your tools to walk from the failure back toward its cause, one step at a time. Prefer the narrowest evidence that answers the next question; do not speculate about code or values you have not seen through a tool.
3. Form ONE root-cause hypothesis that cites the evidence you actually received (tool-returned IDs or file:line locations).
4. Verification: a confirm/replay tool is NOT available in this phase, so your root cause must be labelled unconfirmed. Never present an unconfirmed hypothesis as verified.
5. Finish with a report in exactly this shape:
Root cause: <statement> (unconfirmed)
Evidence: <the key evidence, in causal order, citing IDs or file:line locations>
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: <smallest change>
Regression test: <idea>
ROOT_CAUSE: <fully-qualified-class>#<method>
The ROOT_CAUSE line must name the single class and method where the defect is, from your evidence."""
    }
}
