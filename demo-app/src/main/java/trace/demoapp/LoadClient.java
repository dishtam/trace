package trace.demoapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ADR-0015 load tool: small in-repo Kotlin/Java HTTP client (this one is Java).
 * Usage: LoadClient <baseUrl> <requests> [threads]. Deterministic mix per
 * index: place-order, pricing, stock. With threads > 1 the requests are
 * issued by that many parallel clients (the server-saturating shape used
 * for the T1 measurement; the sequential default cannot resolve agent
 * overhead, see spike-acceptance S0-4). Prints one summary line:
 * LOAD requests=<n> elapsed_ms=<t>
 */
public class LoadClient {
    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "http://localhost:8080";
        int requests = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int threads = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        HttpClient client = HttpClient.newHttpClient();
        long start = System.nanoTime();
        if (threads <= 1) {
            for (int i = 0; i < requests; i++) send(client, base, i);
        } else {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < requests; i++) {
                    int index = i;
                    futures.add(pool.submit(() -> { send(client, base, index); return null; }));
                }
                for (Future<?> future : futures) future.get();
            } finally {
                pool.shutdown();
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("LOAD requests=" + requests + " threads=" + threads + " elapsed_ms=" + elapsedMs);
    }

    private static void send(HttpClient client, String base, int i) {
        HttpRequest request = switch (i % 3) {
            case 0 -> HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"SKU-1\",\"quantity\":1}")).build();
            case 1 -> HttpRequest.newBuilder(URI.create(base + "/pricing?sku=SKU-1&quantity=2")).GET().build();
            default -> HttpRequest.newBuilder(URI.create(base + "/stock/SKU-1")).GET().build();
        };
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("load failed: " + e.getMessage(), e);
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("load failed: " + response.statusCode() + " " + response.body());
        }
    }
}
