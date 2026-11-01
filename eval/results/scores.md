| Bug | Provider | Runner | Claimed root cause | Method | Class | Tokens in/out | USD | Tool calls | Wall |
|---|---|---|---|---|---|---|---|---|---|
| B01 | meta | baseline | trace.demoapp.service.OrderService#placeOrder | yes | yes | 35621/6435 | 0.2712 | 12 | 41755ms |
| B01 | meta | trace | trace.demoapp.service.OrderService#placeOrder | yes | yes | 11398/2402 | 0.0936 | 5 | 17003ms |
| B01 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B01 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B02 | meta | baseline | trace.demoapp.service.TaxService#taxFor | yes | yes | 14166/3427 | 0.1252 | 7 | 19168ms |
| B02 | meta | trace | trace.demoapp.service.TaxService#taxFor | yes | yes | 11704/2957 | 0.1060 | 5 | 16362ms |
| B02 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B02 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B03 | meta | baseline | trace.demoapp.service.PricingService#priceCents | yes | yes | 13876/2682 | 0.1091 | 7 | 16274ms |
| B03 | meta | trace | trace.demoapp.service.PricingService#priceCents | yes | yes | 65262/9034 | 0.4417 | 25 | 56832ms |
| B03 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B03 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B04 | meta | baseline | trace.demoapp.service.InventoryService#decrementChecked | no | yes | 9572/2193 | 0.0821 | 5 | 17159ms |
| B04 | meta | trace | trace.demoapp.service.InventoryService#decrementChecked | no | yes | 14644/2986 | 0.1183 | 7 | 19563ms |
| B04 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B04 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B05 | meta | baseline | trace.demoapp.service.OrderService#listOrders | yes | yes | 20654/3665 | 0.1559 | 7 | 22916ms |
| B05 | meta | trace | trace.demoapp.service.OrderService#listOrders | yes | yes | 72998/7438 | 0.4408 | 33 | 43178ms |
| B05 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B05 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B06 | meta | baseline | trace.demoapp.service.CheckoutService#sumLineTotals | no | yes | 21177/4347 | 0.1716 | 10 | 20117ms |
| B06 | meta | trace | trace.demoapp.service.CheckoutService#sumLineTotals | no | yes | 79538/6648 | 0.4511 | 36 | 46970ms |
| B06 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B06 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B07 | meta | baseline | trace.demoapp.service.PricingService#updateSalePrice | yes | yes | 12912/1966 | 0.0910 | 5 | 12191ms |
| B07 | meta | trace | trace.demoapp.service.PricingService#updateSalePrice | yes | yes | 58101/7542 | 0.3832 | 30 | 47662ms |
| B07 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B07 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B08 | meta | baseline | trace.demoapp.service.OrderService#cancel | yes | yes | 12271/2976 | 0.1086 | 7 | 17420ms |
| B08 | meta | trace | trace.demoapp.service.OrderService#cancel | yes | yes | 75512/10712 | 0.5163 | 35 | 62035ms |
| B08 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B08 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B09 | meta | baseline | trace.demoapp.service.PricingService#priceCents | n/a | yes | 31936/5855 | 0.2448 | 10 | 38865ms |
| B09 | meta | trace | trace.demoapp.service.PricingService#priceCents | n/a | yes | 63900/7154 | 0.3987 | 33 | 44388ms |
| B09 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B09 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B10 | meta | baseline | trace.demoapp.service.OrderService#get | yes | yes | 9566/2963 | 0.0975 | 4 | 16399ms |
| B10 | meta | trace | trace.demoapp.service.OrderService#get | yes | yes | 14496/4075 | 0.1395 | 7 | 30446ms |
| B10 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B10 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B11 | meta | baseline | trace.demoapp.service.OrderService#confirm | yes | yes | 6113/1665 | 0.0578 | 3 | 9178ms |
| B11 | meta | trace | trace.demoapp.service.OrderService#confirm | yes | yes | 26890/5923 | 0.2260 | 19 | 35964ms |
| B11 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B11 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B12 | meta | baseline | trace.demoapp.service.InventoryService#decrementChecked | no | yes | 12622/4114 | 0.1328 | 8 | 22100ms |
| B12 | meta | trace | NOT RUN | - | - | 84536/10535 | 0.5488 | 40 | 58822ms |
| B12 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B12 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B13 | meta | baseline | trace.demoapp.service.OrderService#placeOrder | yes | yes | 9933/2665 | 0.0930 | 6 | 17175ms |
| B13 | meta | trace | trace.demoapp.service.OrderService#placeOrder | yes | yes | 57470/5595 | 0.3418 | 27 | 42853ms |
| B13 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B13 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B14 | meta | baseline | trace.demoapp.service.TaxService#percent | n/a | yes | 28365/4792 | 0.2093 | 16 | 26481ms |
| B14 | meta | trace | trace.demoapp.service.TaxService#percent | n/a | yes | 49139/6849 | 0.3335 | 28 | 45911ms |
| B14 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B14 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B15 | meta | baseline | trace.demoapp.PricingConfig#discountPolicy | yes | yes | 15452/3815 | 0.1381 | 11 | 23695ms |
| B15 | meta | trace | trace.demoapp.PricingConfig#discountPolicy | yes | yes | 29045/8279 | 0.2818 | 17 | 49410ms |
| B15 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B15 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B16 | meta | baseline | trace.demoapp.web.OrderController#place | yes | yes | 14420/3147 | 0.1206 | 7 | 17648ms |
| B16 | meta | trace | trace.demoapp.web.OrderController#place | yes | yes | 43718/7232 | 0.3195 | 23 | 43412ms |
| B16 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B16 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B17 | meta | baseline | trace.demoapp.web.PricingController#priceCallback | yes | yes | 7068/1776 | 0.0638 | 3 | 9097ms |
| B17 | meta | trace | trace.demoapp.web.PricingController#priceCallback | yes | yes | 28885/5151 | 0.2186 | 20 | 32676ms |
| B17 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B17 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B18 | meta | baseline | trace.demoapp.service.PricingService#priceRegionalCents | yes | yes | 11266/3350 | 0.1121 | 6 | 14980ms |
| B18 | meta | trace | trace.demoapp.service.PricingService#priceRegionalCents | yes | yes | 8272/2199 | 0.0771 | 5 | 12438ms |
| B18 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B18 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B19 | meta | baseline | trace.demoapp.service.CheckoutService#shippingCents | yes | yes | 15210/3775 | 0.1363 | 11 | 19637ms |
| B19 | meta | trace | trace.demoapp.service.CheckoutService#shippingCents | yes | yes | 40878/6818 | 0.2999 | 20 | 39912ms |
| B19 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B19 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |
| B20 | meta | baseline | trace.demoapp.service.CheckoutService#checkout | yes | yes | 14799/3806 | 0.1353 | 9 | 18272ms |
| B20 | meta | trace | trace.demoapp.service.CheckoutService#checkout | yes | yes | 15450/3916 | 0.1401 | 6 | 22503ms |
| B20 | anthropic | baseline | NOT RUN | - | - | -/- | - | - | - |
| B20 | anthropic | trace | NOT RUN | - | - | -/- | - | - | - |

- meta/baseline: method 15/20, class 20/20, mean USD 0.1328 over 20 completed investigations
- meta/trace: method 15/19, class 19/19, mean USD 0.2804 over 19 completed investigations
- anthropic/baseline: no completed investigations (0/20 run)
- anthropic/trace: no completed investigations (0/20 run)
