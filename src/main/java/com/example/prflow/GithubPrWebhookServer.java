package com.example.prflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubPrWebhookServer {
    private static final Map<String, String> dotEnv = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        loadDotEnv();

        int port = Integer.parseInt(getEnv("PORT", "8080"));
        String jiraBaseUrl = getEnv("JIRA_BASE_URL");
        String jiraEmail = getEnv("JIRA_BOT_EMAIL");
        String jiraToken = getEnv("JIRA_API_TOKEN");
        String webhookSecret = getEnv("GITHUB_WEBHOOK_SECRET");

        if (jiraBaseUrl == null || jiraEmail == null || jiraToken == null) {
            throw new IllegalStateException("Please set JIRA_BASE_URL, JIRA_BOT_EMAIL, and JIRA_API_TOKEN");
        }

        Map<String, String> transitions = new LinkedHashMap<>();
        transitions.put("develop", System.getenv().getOrDefault("JIRA_TRANSITION_DEVELOP", "42"));
        transitions.put("main", System.getenv().getOrDefault("JIRA_TRANSITION_MAIN", "41"));
        transitions.put("release", System.getenv().getOrDefault("JIRA_TRANSITION_RELEASE", "42"));
        transitions.put("default", System.getenv().getOrDefault("JIRA_TRANSITION_DEFAULT", "42"));

        String epicTransitionId = System.getenv().getOrDefault("JIRA_TRANSITION_EPIC", "");
        String parentIssueKey = System.getenv().getOrDefault("JIRA_PARENT_ISSUE_KEY", "");

        JiraTransitionService transitionService = new JiraTransitionService(jiraBaseUrl, jiraEmail, jiraToken, transitions, epicTransitionId, parentIssueKey);
        JiraKeyExtractor extractor = new JiraKeyExtractor();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/github/pr", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("[webhook] invalid method: " + exchange.getRequestMethod());
                sendJson(exchange, 405, "{\"status\":\"method_not_allowed\"}");
                return;
            }

            String body = readBody(exchange.getRequestBody());
            String signature = exchange.getRequestHeaders().getFirst("X-Hub-Signature-256");
            if (!isSignatureValid(signature, body, webhookSecret)) {
                System.out.println("[webhook] signature invalid or missing");
                sendJson(exchange, 401, "{\"status\":\"unauthorized\"}");
                return;
            }

            String action = extractString(body, "action");
            String pullRequestJson = extractObject(body, "pull_request");
            boolean merged = extractBoolean(pullRequestJson, "merged");
            String baseRef = extractNestedString(pullRequestJson, "base", "ref");
            String headRef = extractNestedString(pullRequestJson, "head", "ref");
            String title = extractString(pullRequestJson, "title");
            if (title.isBlank()) {
                title = extractString(body, "title");
            }
            String bodyText = extractNullableString(pullRequestJson, "body");
            if (bodyText.isBlank()) {
                bodyText = extractNullableString(body, "body");
            }

            System.out.println("[webhook] received event action=" + action + " merged=" + merged + " baseRef=" + baseRef + " headRef=" + headRef);

            if (!"closed".equals(action) || !merged) {
                System.out.println("[webhook] ignored event - not a merged PR");
                sendJson(exchange, 200, "{\"status\":\"ignored\"}");
                return;
            }

            String targetBranch = normalizeBranch(baseRef);
            String combinedText = title + "\n" + bodyText + "\n" + headRef;
            List<String> issues = extractor.extract(combinedText);
            System.out.println("[webhook] extracted issues=" + issues + " from title/body/branch");
            if (issues.isEmpty()) {
                sendJson(exchange, 200, "{\"status\":\"no_jira_key_found\"}");
                return;
            }

            for (String issue : issues) {
                try {
                    System.out.println("[webhook] transitioning issue=" + issue + " branch=" + targetBranch);
                    transitionService.transitionIssue(issue, targetBranch);
                } catch (IOException e) {
                    System.out.println("[webhook] transition failed for " + issue + " error=" + e.getMessage());
                    sendJson(exchange, 500, "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
            }

            sendJson(exchange, 200, "{\"status\":\"ok\",\"branch\":\"" + escapeJson(targetBranch) + "\",\"issues\":" + toJsonArray(issues) + "}");
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Webhook server started on http://localhost:" + port + "/github/pr");
    }

    private static void loadDotEnv() {
        File dotEnvFile = new File(".env");
        if (!dotEnvFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dotEnvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                String key = parts[0].trim();
                String value = parts[1].trim();
                dotEnv.putIfAbsent(key, value);
            }
        } catch (IOException e) {
            System.out.println("[dotenv] failed to read .env file: " + e.getMessage());
        }
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return dotEnv.get(key);
    }

    private static String getEnv(String key, String defaultValue) {
        String value = getEnv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static boolean isSignatureValid(String signature, String body, String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String expected = "sha256=" + hmacSha256(secret, body);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to validate signature", e);
        }
    }

    private static String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String normalizeBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return "";
        }
        return branch.replace("refs/heads/", "").replace("origin/", "");
    }

    private static String toJsonArray(List<String> items) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        boolean first = true;
        for (String item : items) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(item)).append('"');
            first = false;
        }
        builder.append(']');
        return builder.toString();
    }

    private static String extractString(String payload, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\\"((?:\\\\.|[^\\\"]*)*)\\\"|null)");
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            return "";
        }
        String value = matcher.group(2);
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String extractNullableString(String payload, String key) {
        return extractString(payload, key);
    }

    private static boolean extractBoolean(String payload, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(payload);
        return matcher.find() && "true".equals(matcher.group(1));
    }

    private static String extractNestedString(String payload, String parentKey, String childKey) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(parentKey) + "\\\"\\s*:\\s*\\{[^}]*\\\"" + Pattern.quote(childKey) + "\\\"\\s*:\\s*(\\\"((?:\\\\.|[^\\\"]*)*)\\\"|null)");
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            return "";
        }
        String value = matcher.group(2);
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String extractObject(String payload, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\{.*\\})");
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
