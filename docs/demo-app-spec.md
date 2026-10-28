# Demo App — Specification (no code in this task)

Purpose: the Phase 0 workhorse — realistic enough to exercise capture, proxy
safety and async hand-off; small enough to seed 20 known bugs with ground truth.
Stack: Spring Boot (TODO(verify): version at Phase 0 start — pin then), Java
TODO(decide): demo JDK (align with target matrix in architecture.md).

## Shape

- Domain: **orders / inventory / pricing** (orders placed against inventory, priced with rules/tax).
- **Proposed: 15–25 classes** — rationale: big enough for grep to be genuinely hard, small enough to hand-write ground truth. 3 layers:
  - Web: `OrderController`, `InventoryController`, `PricingController` (TODO(decide): exact endpoints)
  - Service: `OrderService`, `InventoryService`, `PricingService`, `TaxService`, `CheckoutService`
  - Data: JPA entities `Order`, `OrderLine`, `Product`, `StockLevel` + repositories
- **One async path:** order confirmation / stock reservation via an executor or `CompletableFuture` (exercises `thread-handoff`).
- **One JPA/lazy path:** an association fetched lazily inside a transaction boundary case (exercises `unsafe_to_render` — rendering it outside a session must be avoided by the agent, skill `instrumentation-safety`).
- **Load script:** repeatable request mix hitting place-order / price / stock endpoints, used for the overhead benchmark (spike-acceptance.md). TODO(decide): tool (e.g. k6/JMeter/plain script) at Phase 0.

## Seeded bugs (20) — each: id, category, trigger, ground truth

Ground-truth location is class/method; line is pinned when the bug is seeded (skill `add-bug-fixture`).

| # | Category | Bug (seeded defect) | Trigger | Ground truth (root cause location) |
|---|---|---|---|---|
| B01 | Null propagation | Repository returns null for unknown SKU; service dereferences it | Order with unknown SKU | `OrderService` — missing null handling of repository result |
| B02 | Null propagation | Tax lookup returns null for zero-rated product; checkout adds it | Zero-rated checkout | `TaxService`/`CheckoutService` — null tax used in total |
| B03 | Wrong conditional | Discount applies when subtotal `<` threshold instead of `>=` | Subtotal exactly at threshold | `PricingService` — inverted comparison |
| B04 | Wrong conditional | Stock check uses `>` instead of `>=` | Order quantity == stock | `InventoryService` — boundary condition |
| B05 | Off-by-one | Pagination skips first element (`from = page*size + 1`-style) | List orders page 1 | `OrderService` — page offset computation |
| B06 | Off-by-one | Loop bound `<` vs `<=` drops last order line from total | Multi-line order total | `CheckoutService` — line iteration bound |
| B07 | Bad state mutation | Price update writes to list price instead of sale price field | Price update then order | `PricingService` — wrong field written |
| B08 | Bad state mutation | Cancelled order still decrements stock (mutation before status check) | Cancel then check stock | `OrderService` — mutation ordering vs status check |
| B09 | Bad state mutation | Shared mutable cart/total object reused across requests | Two sequential orders, totals bleed | Service singleton state field (TODO(decide): exact shape when seeding) |
| B10 | Exception swallowed | Repository exception caught, empty result returned, NPE later | DB/constraint failure injection (TODO(decide): injection method) | Service catch block — swallowed exception, cause lost |
| B11 | Exception swallowed | Async task exception never observed (Future never checked) | Async confirmation with failing input | Async submission site — unchecked Future |
| B12 | Concurrency/race | Check-then-act stock decrement without atomicity | Parallel orders for last unit | `InventoryService` — non-atomic check+decrement |
| B13 | Concurrency/race | Async handoff loses context (order id not propagated) | Async confirmation for order | Async task — captured variable/context bug (TODO(decide when seeding) |
| B14 | Config/env | Reads `pricing.tax.rate` instead of `pricing.vat.rate`-style wrong key | App started with test profile | Config binding class — wrong property key |
| B15 | Config/env | Profile-specific bean overrides pricing in all profiles | Any priced order under default profile | Configuration class — missing profile condition |
| B16 | Serialization/mapping | DTO mapping drops `discount` field | Price via API response | Mapper — field not copied |
| B17 | Serialization/mapping | JSON field rename breaks deserialization of callback payload | Pricing callback payload | DTO annotations — name mismatch |
| B18 | Null propagation | Map lookup with wrong key returns null, propagated to response | Pricing for unknown region | `PricingService` — wrong map key |
| B19 | Wrong conditional | Free-shipping threshold compares pre-discount instead of post-discount total | Order near threshold with discount | `CheckoutService` — wrong value compared |
| B20 | Exception swallowed | Exception cause chain replaced by new exception without cause | Failing payment step (TODO(decide: stub) | Service — `throw new X(...)` without cause |

TODO(decide): B09/B10/B13/B20 exact seeding mechanics when the app exists — do not invent framework details now.
Each bug ships with: failing trigger, passing (clean) variant for `diff_runs`, and a `fixture.md` per evaluation.md.
