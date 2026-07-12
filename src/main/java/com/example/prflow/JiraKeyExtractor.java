package com.example.prflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraKeyExtractor {

    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("([A-Z][A-Z0-9_]+-\\d+)");

    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        Set<String> keys = new HashSet<>();
        Matcher matcher = JIRA_KEY_PATTERN.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return new ArrayList<>(keys);
    }
}
