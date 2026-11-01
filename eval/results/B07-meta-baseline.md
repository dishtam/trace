---
bug: B07
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#updateSalePrice
budget: tokens_in=12912 tokens_out=1966 cost_usd=0.0910 tool_calls=5
budget_exceeded: false
wall_ms: 12191
tool_calls_log:
  - search_text({"pattern":"sale-price"}) -> java\trace\demoapp\web\PricingController.java:28: @PutMapping("/products/{sku}/sale-price")
  - read_file({"path":"java/trace/demoapp/web/PricingController.java"}) -> java/trace/demoapp/web/PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - search_text({"pattern":"updateSalePrice"}) -> java\trace\demoapp\service\PricingService.java:62: public Product updateSalePrice(String sku, long salePriceCents) { jav
  - read_file({"path":"java/trace/demoapp/service/PricingService.java"}) -> java/trace/demoapp/service/PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/data/Product.java"}) -> java/trace/demoapp/data/Product.java lines 1..10 of 10 package trace.demoapp.data; import jakarta.persistence.*; @Entity
---

Root cause: PricingService.updateSalePrice writes the sale price to the list-price field when b07 is active, leaving salePriceCents at 0 and persisting the corrupted list price (unconfirmed)
Evidence: Bug report B07 PUT /products/SKU-1/sale-price with 500 returned {"listPriceCents":500,"salePriceCents":0} and subsequent GET returned the same instead of list 1000/sale 500; PricingController.java:28-30 delegates to pricing.updateSalePrice; PricingService.java:64-66 `if (seeded.active("b07")) product.priceCents = salePriceCents; else product.salePriceCents = salePriceCents;` and PricingService.java:67 saves it; Product.java:6-8 defines priceCents as list price and salePriceCents as sale price; PricingController.java:36-38 maps those fields to listPriceCents/salePriceCents in the response
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In PricingService.java:65-66 always assign `product.salePriceCents = salePriceCents;` and never assign it to product.priceCents in updateSalePrice
Regression test: Seed SKU-1 with listPriceCents 1000, PUT sale-price 500, then GET /products/SKU-1 and assert listPriceCents=1000 and salePriceCents=500
ROOT_CAUSE: trace.demoapp.service.PricingService#updateSalePrice
