package com.example.prflow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraKeyExtractor {
    private static final Pattern ISSUE_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9_]+-\\d+)\\b");

    public List<String> extract(String text) {
        List<String> issues = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return issues;
        }

        Matcher matcher = ISSUE_PATTERN.matcher(text);
        while (matcher.find()) {
            String issueKey = matcher.group(1);
            if (!issues.contains(issueKey)) {
                issues.add(issueKey);
            }
        }
        return issues;
    }
}
