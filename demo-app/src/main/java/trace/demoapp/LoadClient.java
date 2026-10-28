package trace.demoapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ADR-0015 load tool: small in-repo Kotlin/Java HTTP client (this one is Java).
 * Usage: LoadClient <baseUrl> <requests>. Deterministic mix per index:
 * place-order, pricing, stock. Prints one summary line: LOAD requests=<n> elapsed_ms=<t>
 */
public class LoadClient {
    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "http://localhost:8080";
        int requests = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        HttpClient client = HttpClient.newHttpClient();
        long start = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            HttpRequest request = switch (i % 3) {
                case 0 -> HttpRequest.newBuilder(URI.create(base + "/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"SKU-1\",\"quantity\":1}")).build();
                case 1 -> HttpRequest.newBuilder(URI.create(base + "/pricing?sku=SKU-1&quantity=2")).GET().build();
                default -> HttpRequest.newBuilder(URI.create(base + "/stock/SKU-1")).GET().build();
            };
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("load failed: " + response.statusCode() + " " + response.body());
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("LOAD requests=" + requests + " elapsed_ms=" + elapsedMs);
    }
}
