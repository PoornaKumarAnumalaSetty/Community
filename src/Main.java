import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/issues", new IssuesHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Server running at http://localhost:" + port);
        server.start();
    }

    // In-memory store
    static class Store {
        static final AtomicLong idSeq = new AtomicLong(1);
        static final Map<Long, Issue> issues = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    static class Issue {
        long id;
        String title;
        String description;
        int votes;
        long createdAt;
        List<Comment> comments = new ArrayList<>();
    }

    static class Comment {
        String text;
        long createdAt;
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/"))
                path = "/index.html";
            Path file = Paths.get("public" + path).normalize();
            if (!file.startsWith(Paths.get("public"))) {
                sendText(exchange, 403, "Forbidden");
                return;
            }
            if (!Files.exists(file) || Files.isDirectory(file)) {
                sendText(exchange, 404, "Not Found");
                return;
            }
            String contentType = guessContentType(file.toString());
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", contentType);
            byte[] bytes = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class IssuesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (method.equals("GET") && path.equals("/api/issues")) {
                sendJson(exchange, 200, serializeIssues());
                return;
            }

            if (method.equals("POST") && path.equals("/api/issues")) {
                Map<String, String> form = parseForm(exchange.getRequestBody());
                String title = safe(form.get("title"));
                String description = safe(form.get("description"));
                if (title.isEmpty() || description.isEmpty()) {
                    sendText(exchange, 400, "Title and description required");
                    return;
                }
                Issue issue = new Issue();
                issue.id = Store.idSeq.getAndIncrement();
                issue.title = title;
                issue.description = description;
                issue.votes = 0;
                issue.createdAt = Instant.now().toEpochMilli();
                Store.issues.put(issue.id, issue);
                sendJson(exchange, 201, serializeIssue(issue));
                return;
            }

            // /api/issues/{id}/vote or /api/issues/{id}/comments
            String[] parts = path.split("/");
            if (parts.length >= 4 && parts[1].equals("api") && parts[2].equals("issues")) {
                try {
                    long id = Long.parseLong(parts[3]);
                    Issue issue = Store.issues.get(id);
                    if (issue == null) {
                        sendText(exchange, 404, "Not found");
                        return;
                    }

                    if (parts.length == 5 && parts[4].equals("vote") && method.equals("POST")) {
                        issue.votes += 1;
                        sendJson(exchange, 200, serializeIssue(issue));
                        return;
                    }

                    if (parts.length == 5 && parts[4].equals("comments") && method.equals("POST")) {
                        Map<String, String> form = parseForm(exchange.getRequestBody());
                        String text = safe(form.get("text"));
                        if (text.isEmpty()) {
                            sendText(exchange, 400, "Text required");
                            return;
                        }
                        Comment c = new Comment();
                        c.text = text;
                        c.createdAt = Instant.now().toEpochMilli();
                        issue.comments.add(c);
                        sendJson(exchange, 201, serializeIssue(issue));
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendText(exchange, 400, "Invalid id");
                    return;
                }
            }

            sendText(exchange, 404, "Not Found");
        }
    }

    // Helpers
    static Map<String, String> parseForm(InputStream is) throws IOException {
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty())
                continue;
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String val = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(key, val);
        }
        return map;
    }

    static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String safe(String s) {
        if (s == null)
            return "";
        String trimmed = s.trim();
        String sanitized = trimmed.replaceAll("[\\r\\n]+", " ");
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        return sanitized;
    }

    static String guessContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html; charset=utf-8";
        if (path.endsWith(".css"))
            return "text/css; charset=utf-8";
        if (path.endsWith(".js"))
            return "application/javascript; charset=utf-8";
        if (path.endsWith(".json"))
            return "application/json; charset=utf-8";
        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        return "application/octet-stream";
    }

    static String serializeIssues() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        synchronized (Store.issues) {
            for (Issue i : Store.issues.values()) {
                if (!first)
                    sb.append(",");
                sb.append(serializeIssue(i));
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String serializeIssue(Issue i) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"id\":").append(i.id).append(",")
                .append("\"title\":\"").append(jsonEscape(i.title)).append("\",")
                .append("\"description\":\"").append(jsonEscape(i.description)).append("\",")
                .append("\"votes\":").append(i.votes).append(",")
                .append("\"createdAt\":").append(i.createdAt).append(",")
                .append("\"comments\":[");
        boolean first = true;
        for (Comment c : i.comments) {
            if (!first)
                sb.append(",");
            sb.append("{")
                    .append("\"text\":\"").append(jsonEscape(c.text)).append("\",")
                    .append("\"createdAt\":").append(c.createdAt)
                    .append("}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    static void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
