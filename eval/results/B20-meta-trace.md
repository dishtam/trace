---
bug: B20
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.CheckoutService#checkout
budget: tokens_in=15450 tokens_out=3916 cost_usd=0.1401 tool_calls=6
budget_exceeded: false
wall_ms: 22503
tool_calls_log:
  - get_failure_point({"run_id":"da0ddb96-c786-4e1b-9f3b-4600d0c436b8"}) -> {"summary":"java.lang.IllegalStateException at trace.demoapp.service.CheckoutService#checkout; 3 frames","items":[{"even
  - trace_back({"run_id":"da0ddb96-c786-4e1b-9f3b-4600d0c436b8","target_id":35}) -> {"summary":"4 slice events from event 35 (depth <= 20)","items":[{"event_id":35,"kind":"throw","invocation_id":14,"where
  - get_call_subtree({"run_id":"da0ddb96-c786-4e1b-9f3b-4600d0c436b8","invocation_id":14}) -> {"summary":"4 invocations under 14 (depth <= 3)","items":[{"invocation_id":14,"parent_invocation_id":12,"call_event_id":
  - get_source({"class_name":"trace.demoapp.service.CheckoutService","method_name":"checkout"}) -> {"summary":"source for trace.demoapp.service.CheckoutService#checkout at CheckoutService.java:23","items":[{"symbol_id":
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"priceCents"}) -> {"summary":"source for trace.demoapp.service.PricingService#priceCents at PricingService.java:29","items":[{"symbol_id":
  - get_call_subtree({"run_id":"da0ddb96-c786-4e1b-9f3b-4600d0c436b8","invocation_id":15}) -> {"summary":"2 invocations under 15 (depth <= 3)","items":[{"invocation_id":15,"parent_invocation_id":14,"call_event_id":
---

Root cause: In CheckoutService#checkout, the catch block replaces the original RuntimeException from pricing with `new IllegalStateException("checkout failed for " + sku)` without passing it as cause, swallowing the `IllegalArgumentException: unknown sku UNKNOWN` (unconfirmed)
Evidence: OrderController#place event_id 20 -> OrderService#placeOrder event_id 25 -> CheckoutService#checkout event_id 28/invocation_id 14; subtree of invocation_id 14 shows PricingService#priceCents invocation_id 15 outcome throw and SeededBugs#active invocation_id 17; PricingService.java:32 throws `IllegalArgumentException("unknown sku " + sku)`; CheckoutService.java:29-32 catches RuntimeException and at CheckoutService.java:31 throws the bare IllegalStateException; failure point event_id 35 is `IllegalStateException: checkout failed for UNKNOWN` at trace.demoapp.service.CheckoutService#checkout
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Remove the seeded b20 replacement in CheckoutService#checkout so the catch block rethrows the original `failure` (or at minimum pass `failure` as the cause)
Regression test: Call CheckoutService#checkout / POST /orders with sku UNKNOWN and quantity 1, assert the propagated exception is IllegalArgumentException with message "unknown sku UNKNOWN" rather than a causeless IllegalStateException "checkout failed for UNKNOWN"
ROOT_CAUSE: trace.demoapp.service.CheckoutService#checkout
