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

import app.login.LoginRequest;
import app.login.LoginService;

public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        DatabaseConfig databaseConfig = DatabaseConfig.fromEnvironment();

        RegistrationService registrationService = new RegistrationService(databaseConfig);

        LoginService loginService = new LoginService(databaseConfig);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", Application::handleRoot);
        server.createContext("/login", Application::handleRoot);
        server.createContext("/registration", Application::handleRoot);
        server.createContext("/api/register", exchange ->
        handleRegistration(exchange, registrationService));
        server.createContext("/api/login", exchange ->
        handleLogin(exchange, loginService));
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
            case "/", "/login" -> "/static/login/index.html";
            case "/login/styles.css" -> "/static/login/styles.css";
            case "/login/app.js" -> "/static/login/app.js";
            case "/registration" -> "/static/registration/index.html";
            case "/registration/styles.css" -> "/static/registration/styles.css";
            case "/registration/app.js" -> "/static/registration/app.js";
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

    private static void handleLogin(
        HttpExchange exchange,
        LoginService loginService
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

        Map<String, String> fields = parseJsonObject(body);

        LoginRequest request = new LoginRequest(
                fields.get("email"),
                fields.get("password")
        );

        boolean authenticated = loginService.authenticate(request);

        if (!authenticated) {
            sendText(exchange, 401, "Invalid email or password");
            return;
        }

        sendEmpty(exchange, 200);
    } catch (IllegalArgumentException exception) {
        sendText(exchange, 400, exception.getMessage());
    } catch (Exception exception) {
        sendText(exchange, 500, "Login could not be completed");
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
        if (path.endsWith(".css")) {
        return "text/css; charset=UTF-8";
        }

        if (path.endsWith(".js")) {
        return "text/javascript; charset=UTF-8";
        }

        return "text/html; charset=UTF-8";

    }

    private static Map<String, String> parseJsonObject(String json) {
    Map<String, String> fields = new HashMap<>();
    int[] index = {0};

    skipWhitespace(json, index);
    expectCharacter(json, index, '{');
    skipWhitespace(json, index);

    if (peekCharacter(json, index) == '}') {
        index[0]++;
        return fields;
    }

    while (true) {
        String key = readJsonString(json, index);

        skipWhitespace(json, index);
        expectCharacter(json, index, ':');

        String value = readJsonString(json, index);
        fields.put(key, value);

        skipWhitespace(json, index);

        char next = peekCharacter(json, index);

        if (next == '}') {
            index[0]++;
            break;
        }

        expectCharacter(json, index, ',');
        skipWhitespace(json, index);
    }

    skipWhitespace(json, index);

    if (index[0] != json.length()) {
        throw new IllegalArgumentException("Invalid JSON request");
    }

    return fields;
}

    private static String readJsonString(String json, int[] index) {
        skipWhitespace(json, index);
        expectCharacter(json, index, '"');

        StringBuilder value = new StringBuilder();

        while (index[0] < json.length()) {
            char character = json.charAt(index[0]++);

            if (character == '"') {
                return value.toString();
            }

            if (character != '\\') {
                value.append(character);
                continue;
            }

            if (index[0] >= json.length()) {
                throw new IllegalArgumentException("Invalid JSON string");
            }

            char escapedCharacter = json.charAt(index[0]++);

            switch (escapedCharacter) {
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                case '/' -> value.append('/');
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (index[0] + 4 > json.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape");
                    }

                    String hexadecimal = json.substring(
                            index[0],
                            index[0] + 4
                    );

                    try {
                        value.append((char) Integer.parseInt(hexadecimal, 16));
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException(
                                "Invalid JSON escape",
                                exception
                        );
                    }

                    index[0] += 4;
                }
                default -> throw new IllegalArgumentException(
                        "Invalid JSON escape"
                );
            }
        }

        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static void skipWhitespace(String json, int[] index) {
        while (index[0] < json.length()
                && Character.isWhitespace(json.charAt(index[0]))) {
            index[0]++;
        }
    }

    private static void expectCharacter(
            String json,
            int[] index,
            char expected
    ) {
        if (index[0] >= json.length()
                || json.charAt(index[0]) != expected) {
            throw new IllegalArgumentException("Invalid JSON request");
        }

        index[0]++;
    }

    private static char peekCharacter(String json, int[] index) {
        if (index[0] >= json.length()) {
            throw new IllegalArgumentException("Invalid JSON request");
        }

        return json.charAt(index[0]);
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

    private static void sendEmpty(HttpExchange exchange, int status)
        throws IOException {
    exchange.sendResponseHeaders(status, 0);
    exchange.close();
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