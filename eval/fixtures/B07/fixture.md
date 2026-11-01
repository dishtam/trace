# B07 - Bad state mutation (seeded, profile `b07`)

- id: B07
- category: Bad state mutation across calls
- source: seeded (demo-app-spec.md B07)
- activation: Spring profile `b07`
- trigger: `PUT /products/SKU-1/sale-price` with 500, then
  `GET /products/SKU-1`.
  Seeded symptom: listPriceCents becomes 500 and salePriceCents stays 0
  (clean: listPriceCents 1000, salePriceCents 500).
- ground-truth root cause location: `trace.demoapp.service.PricingService#updateSalePrice`
  - writes the list-price field instead of the sale-price field
    (seeded line marked SEEDED-B07)
- ground-truth explanation: The sale-price update assigns the new value
  to Product.priceCents (the list price) instead of
  Product.salePriceCents, corrupting the list price and leaving the sale
  price unset. Pricing itself is unchanged; the written field is wrong.
- expected backward slice: the Product state read back by GET
  /products/SKU-1; its producing write in
  PricingService.updateSalePrice.
- passing-run availability: yes - default profile (clean) writes the
  sale-price field.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
