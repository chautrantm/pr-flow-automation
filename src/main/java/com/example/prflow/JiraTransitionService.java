package com.example.prflow;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

public class JiraTransitionService {

    private static final String TRANSITIONS_PATH_TEMPLATE = "/rest/api/3/issue/%s/transitions";
    private static final String TRANSITION_PAYLOAD_TEMPLATE = "{\"transition\":{\"id\":\"%s\"}}";

    private final String jiraBaseUrl;
    private final String botEmail;
    private final String apiToken;
    private final Map<String, String> transitionByBranch;
    private final String epicTransitionId;
    private final String parentIssueKey;

    public JiraTransitionService(String jiraBaseUrl, String botEmail, String apiToken, Map<String, String> transitionByBranch) {
        this(jiraBaseUrl, botEmail, apiToken, transitionByBranch, "", "");
    }

    public JiraTransitionService(String jiraBaseUrl, String botEmail, String apiToken, Map<String, String> transitionByBranch, String epicTransitionId, String parentIssueKey) {
        this.jiraBaseUrl = jiraBaseUrl;
        this.botEmail = botEmail;
        this.apiToken = apiToken;
        this.transitionByBranch = transitionByBranch;
        this.epicTransitionId = epicTransitionId;
        this.parentIssueKey = parentIssueKey;
    }

    public void transitionIssue(String issueKey, String targetBranch) throws IOException {
        String transitionId = resolveTransitionId(targetBranch);
        if (transitionId == null || transitionId.isBlank()) {
            throw new IllegalArgumentException("No transition configured for branch: " + targetBranch);
        }

        transitionIssueInternal(issueKey, transitionId);

        if (shouldTransitionParentIssue()) {
            transitionIssueInternal(parentIssueKey, epicTransitionId);
        }
    }

    private boolean shouldTransitionParentIssue() {
        return !parentIssueKey.isBlank() && !epicTransitionId.isBlank();
    }

    private void transitionIssueInternal(String issueKey, String transitionId) throws IOException {
        String url = jiraBaseUrl + String.format(TRANSITIONS_PATH_TEMPLATE, issueKey);
        String payload = String.format(TRANSITION_PAYLOAD_TEMPLATE, transitionId);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Basic " + basicAuth(botEmail, apiToken));
            connection.setDoOutput(true);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readBody(connection);
            System.out.println("[jira] issue=" + issueKey + " transitionId=" + transitionId + " status=" + statusCode + " response=" + responseBody);
            if (statusCode >= 400) {
                throw new IOException("Jira transition failed: " + statusCode + " " + responseBody);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String resolveTransitionId(String targetBranch) {
        if (targetBranch == null || targetBranch.isBlank()) {
            return transitionByBranch.get("default");
        }

        String normalized = targetBranch.trim().replace("refs/heads/", "").replace("origin/", "").toLowerCase(Locale.ROOT);
        if (normalized.startsWith("release/")) {
            return transitionByBranch.getOrDefault("release", transitionByBranch.get("default"));
        }
        return transitionByBranch.getOrDefault(normalized, transitionByBranch.get("default"));
    }

    private String readBody(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream()) {
            if (inputStream == null) {
                return "";
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String basicAuth(String email, String token) {
        String value = email + ":" + token;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
