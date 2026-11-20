package sumdu.edu.ua.web.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResendStub {

    public record Request(String authorization, String contentType, JsonNode body) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String response = "{\"id\":\"test-email-id\"}";

    public ResendStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/emails", exchange -> {
            byte[] in = exchange.getRequestBody().readAllBytes();
            requests.add(new Request(
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    JSON.readTree(in)));
            byte[] out = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/emails";
    }

    public List<Request> requests() {
        return requests;
    }

    public void reset() {
        requests.clear();
        status = 200;
        response = "{\"id\":\"test-email-id\"}";
    }

    public void respondWith(int status, String response) {
        this.status = status;
        this.response = response;
    }
}
