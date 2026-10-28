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
}
