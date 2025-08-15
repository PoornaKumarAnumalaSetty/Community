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
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    private static DatabaseManager dbManager;

    public static void main(String[] args) throws Exception {
        // Load database configuration
        Properties config = loadDatabaseConfig();

        // Initialize database connection
        dbManager = new DatabaseManager(config);
        dbManager.initializeDatabase();

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/issues", new IssuesHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println("🚀 Community Board Server Starting...");
        System.out.println("📊 Database: " + config.getProperty("db.url"));
        System.out.println("🌐 Server running at http://localhost:" + port);
        System.out.println("👥 Ready to serve your community!");

        server.start();

        // Add shutdown hook to close database connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🔄 Shutting down server...");
            if (dbManager != null) {
                dbManager.close();
            }
            System.out.println("✅ Server stopped gracefully");
        }));
    }

    // Database Manager Class
    static class DatabaseManager {
        private final Properties config;
        private Connection connection;

        public DatabaseManager(Properties config) {
            this.config = config;
        }

        public void initializeDatabase() throws SQLException {
            try {
                // Load JDBC driver
                Class.forName(config.getProperty("db.driver"));

                // Create connection
                connection = DriverManager.getConnection(
                        config.getProperty("db.url"),
                        config.getProperty("db.user"),
                        config.getProperty("db.password"));

                // Create tables if they don't exist
                createTables();

                System.out.println("✅ Database connected successfully");

            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "PostgreSQL JDBC driver not found. Please ensure the driver is in the lib/ folder.", e);
            }
        }

        private void createTables() throws SQLException {
            String createIssuesTable = """
                    CREATE TABLE IF NOT EXISTS issues (
                        id SERIAL PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        votes INTEGER DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            String createCommentsTable = """
                    CREATE TABLE IF NOT EXISTS comments (
                        id SERIAL PRIMARY KEY,
                        issue_id INTEGER REFERENCES issues(id) ON DELETE CASCADE,
                        text TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createIssuesTable);
                stmt.execute(createCommentsTable);
            }
        }

        public List<Issue> getAllIssues() throws SQLException {
            List<Issue> issues = new ArrayList<>();
            String sql = """
                    SELECT i.id, i.title, i.description, i.votes, i.created_at,
                           c.id as comment_id, c.text as comment_text, c.created_at as comment_created
                    FROM issues i
                    LEFT JOIN comments c ON i.id = c.issue_id
                    ORDER BY i.created_at DESC, c.created_at ASC
                    """;

            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                Map<Long, Issue> issueMap = new LinkedHashMap<>();

                while (rs.next()) {
                    long issueId = rs.getLong("id");
                    Issue issue = issueMap.get(issueId);

                    if (issue == null) {
                        issue = new Issue();
                        issue.id = issueId;
                        issue.title = rs.getString("title");
                        issue.description = rs.getString("description");
                        issue.votes = rs.getInt("votes");
                        issue.createdAt = rs.getTimestamp("created_at").getTime();
                        issue.comments = new ArrayList<>();
                        issueMap.put(issueId, issue);
                    }

                    // Add comment if exists
                    if (rs.getObject("comment_id") != null) {
                        Comment comment = new Comment();
                        comment.text = rs.getString("comment_text");
                        comment.createdAt = rs.getTimestamp("comment_created").getTime();
                        issue.comments.add(comment);
                    }
                }

                issues.addAll(issueMap.values());
            }

            return issues;
        }

        public Issue createIssue(String title, String description) throws SQLException {
            String sql = "INSERT INTO issues (title, description) VALUES (?, ?) RETURNING id, created_at";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, title);
                pstmt.setString(2, description);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Issue issue = new Issue();
                        issue.id = rs.getLong("id");
                        issue.title = title;
                        issue.description = description;
                        issue.votes = 0;
                        issue.createdAt = rs.getTimestamp("created_at").getTime();
                        issue.comments = new ArrayList<>();
                        return issue;
                    }
                }
            }

            throw new SQLException("Failed to create issue");
        }

        public Issue voteIssue(long id) throws SQLException {
            String sql = "UPDATE issues SET votes = votes + 1 WHERE id = ? RETURNING *";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, id);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Issue issue = new Issue();
                        issue.id = rs.getLong("id");
                        issue.title = rs.getString("title");
                        issue.description = rs.getString("description");
                        issue.votes = rs.getInt("votes");
                        issue.createdAt = rs.getTimestamp("created_at").getTime();

                        // Load comments for this issue
                        issue.comments = getCommentsForIssue(id);
                        return issue;
                    }
                }
            }

            throw new SQLException("Issue not found");
        }

        public Issue addComment(long issueId, String text) throws SQLException {
            // First add the comment
            String insertComment = "INSERT INTO comments (issue_id, text) VALUES (?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(insertComment)) {
                pstmt.setLong(1, issueId);
                pstmt.setString(2, text);
                pstmt.executeUpdate();
            }

            // Then return the updated issue with all comments
            return getIssueById(issueId);
        }

        private Issue getIssueById(long id) throws SQLException {
            String sql = "SELECT * FROM issues WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, id);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Issue issue = new Issue();
                        issue.id = rs.getLong("id");
                        issue.title = rs.getString("title");
                        issue.description = rs.getString("description");
                        issue.votes = rs.getInt("votes");
                        issue.createdAt = rs.getTimestamp("created_at").getTime();
                        issue.comments = getCommentsForIssue(id);
                        return issue;
                    }
                }
            }

            throw new SQLException("Issue not found");
        }

        private List<Comment> getCommentsForIssue(long issueId) throws SQLException {
            List<Comment> comments = new ArrayList<>();
            String sql = "SELECT * FROM comments WHERE issue_id = ? ORDER BY created_at ASC";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, issueId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Comment comment = new Comment();
                        comment.text = rs.getString("text");
                        comment.createdAt = rs.getTimestamp("created_at").getTime();
                        comments.add(comment);
                    }
                }
            }

            return comments;
        }

        public void close() {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("✅ Database connection closed");
                } catch (SQLException e) {
                    System.err.println("❌ Error closing database connection: " + e.getMessage());
                }
            }
        }
    }

    // Data Models
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

    // Static File Handler
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

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

    // API Handler
    static class IssuesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if (method.equals("GET") && path.equals("/api/issues")) {
                    List<Issue> issues = dbManager.getAllIssues();
                    sendJson(exchange, 200, serializeIssues(issues));
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

                    Issue issue = dbManager.createIssue(title, description);
                    sendJson(exchange, 201, serializeIssue(issue));
                    return;
                }

                // Handle /api/issues/{id}/vote or /api/issues/{id}/comments
                String[] parts = path.split("/");
                if (parts.length >= 4 && parts[1].equals("api") && parts[2].equals("issues")) {
                    try {
                        long id = Long.parseLong(parts[3]);

                        if (parts.length == 5 && parts[4].equals("vote") && method.equals("POST")) {
                            Issue issue = dbManager.voteIssue(id);
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

                            Issue issue = dbManager.addComment(id, text);
                            sendJson(exchange, 201, serializeIssue(issue));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendText(exchange, 400, "Invalid id");
                        return;
                    }
                }

                sendText(exchange, 404, "Not Found");

            } catch (SQLException e) {
                System.err.println("❌ Database error: " + e.getMessage());
                sendText(exchange, 500, "Database error occurred");
            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
                sendText(exchange, 500, "Internal server error");
            }
        }
    }

    // Helper Methods
    static Properties loadDatabaseConfig() throws IOException {
        Properties props = new Properties();
        Path configFile = Paths.get("database.properties");

        if (!Files.exists(configFile)) {
            throw new IOException(
                    "database.properties file not found. Please create it with your database credentials.");
        }

        try (InputStream is = Files.newInputStream(configFile)) {
            props.load(is);
        }

        // Validate required properties
        String[] required = { "db.url", "db.user", "db.password", "db.driver" };
        for (String prop : required) {
            if (!props.containsKey(prop) || props.getProperty(prop).trim().isEmpty()) {
                throw new IOException("Missing or empty required property: " + prop);
            }
        }

        return props;
    }

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

    static String serializeIssues(List<Issue> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean first = true;
        for (Issue i : issues) {
            if (!first)
                sb.append(",");
            sb.append(serializeIssue(i));
            first = false;
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
