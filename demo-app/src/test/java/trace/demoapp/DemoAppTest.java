package trace.demoapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DemoAppTest {
    @Autowired MockMvc mvc;

    @Test void oneRealisticRequestPath() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCents").value(2200));
        mvc.perform(get("/pricing").param("sku", "SKU-1").param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCents").value(2200));
        mvc.perform(get("/stock/SKU-1")).andExpect(status().isOk());
        mvc.perform(get("/orders/1/lines")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCount").value(1));
    }

    @Test void listAndGetOrderClean() throws Exception {
        String body = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mvc.perform(get("/orders").param("page", "0").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
        mvc.perform(get("/orders/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCents").value(1100));
        // Clean tax/B-pricing expectations the fixtures rely on:
        mvc.perform(get("/pricing").param("sku", "SKU-1").param("quantity", "5"))
                .andExpect(jsonPath("$.totalCents").value(4950)); // discount at threshold
        mvc.perform(get("/pricing").param("sku", "SKU-2").param("quantity", "1"))
                .andExpect(jsonPath("$.totalCents").value(2500)); // zero-rated
    }

    private int stockOf(String sku) throws Exception {
        String body = mvc.perform(get("/stock/" + sku)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(body.replaceAll(".*\"available\":(\\d+).*", "$1"));
    }

    @Test void newEndpointsCleanBehaviour() throws Exception {
        // B07 surface: sale price goes to the sale field, list price untouched.
        mvc.perform(put("/products/SKU-2/sale-price").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salePriceCents\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listPriceCents").value(2500))
                .andExpect(jsonPath("$.salePriceCents").value(2000));
        mvc.perform(get("/pricing").param("sku", "SKU-2").param("quantity", "1"))
                .andExpect(jsonPath("$.totalCents").value(2000)); // zero-rated, sale price applies
        mvc.perform(put("/products/SKU-2/sale-price").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salePriceCents\":0}"))
                .andExpect(jsonPath("$.salePriceCents").value(0));
        // B18 surface: regional pricing resolves the region rate.
        mvc.perform(get("/pricing/regional").param("sku", "SKU-1").param("quantity", "1").param("region", "EU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCents").value(1100));
        // B17 surface: callback field round-trips.
        mvc.perform(post("/pricing/callback").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"totalCents\":1234}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedTotalCents").value(1234));
        // B19 surface: post-discount total (4500) is below the free-shipping threshold.
        mvc.perform(get("/checkout/shipping").param("sku", "SKU-2").param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingCents").value(500));
        // B08 surface: cancelling restores the placed stock.
        int before = stockOf("SKU-1");
        String placed = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"quantity\":1}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(placed.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mvc.perform(post("/orders/" + id + "/cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        org.junit.jupiter.api.Assertions.assertEquals(before, stockOf("SKU-1"));
        // B11/B13 surface: a placed order is confirmed; confirming a missing order fails loudly.
        mvc.perform(get("/orders/" + id + "/confirmation")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        // Real HTTP returns 500 here; MockMvc rethrows the unhandled exception instead.
        org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class,
                () -> mvc.perform(post("/orders/9999/confirm")));
    }
}
