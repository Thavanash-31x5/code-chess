package app;

import app.database.DatabaseConfig;
import app.registration.DuplicateEmailException;
import app.registration.RegistrationRequest;
import app.registration.RegistrationService;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        RegistrationService registrationService =
                new RegistrationService(DatabaseConfig.fromEnvironment());

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", Application::handleRoot);
        server.createContext("/api/register", exchange ->
                handleRegistration(exchange, registrationService));
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port " + port);
    }

    private static void handleRoot(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendText(exchange, 404, "Not found");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String resourcePath = switch (path) {
            case "/" -> "/static/index.html";
            case "/styles.css" -> "/static/styles.css";
            case "/app.js" -> "/static/app.js";
            default -> null;
        };

        if (resourcePath == null) {
            sendText(exchange, 404, "Not found");
            return;
        }

        try (InputStream resource = Application.class.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                sendText(exchange, 500, "Frontend resource is unavailable");
                return;
            }

            byte[] response = resource.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", contentType(path));
            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

    }
    private static void handleRegistration(
            HttpExchange exchange,
            RegistrationService registrationService
    ) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String body;
            try (InputStream input = exchange.getRequestBody()) {
                body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            Map<String, String> fields = parseForm(body);
            RegistrationRequest request = new RegistrationRequest(
                    fields.get("name"),
                    fields.get("phoneNumber"),
                    fields.get("email"),
                    fields.get("password")
            );

            registrationService.register(request);
            sendText(exchange, 201, "Registration successful");
        } catch (IllegalArgumentException exception) {
            sendText(exchange, 400, exception.getMessage());
        } catch (DuplicateEmailException exception) {
            sendText(exchange, 409, "An account already exists for this email");
        } catch (Exception exception) {
            sendText(exchange, 500, "Registration could not be completed");
        }
    }

    private static String contentType(String path) {
        return switch (path) {
            case "/styles.css" -> "text/css; charset=UTF-8";
            case "/app.js" -> "text/javascript; charset=UTF-8";
            default -> "text/html; charset=UTF-8";
        };
    }

    private static Map<String, String> parseForm(String body) {
        return java.util.Arrays.stream(body.split("&"))
                .filter(pair -> !pair.isBlank())
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> decode(parts[0]),
                        parts -> decode(parts[1]),
                        (first, second) -> second,
                        HashMap::new
                ));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendText(HttpExchange exchange, int status, String message)
            throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

}