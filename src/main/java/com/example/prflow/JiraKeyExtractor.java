package com.example.prflow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraKeyExtractor {
    private static final Pattern ISSUE_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9_]+-\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("browse/([A-Z][A-Z0-9_]+-\\d+)", Pattern.CASE_INSENSITIVE);

    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (Matcher matcher = ISSUE_PATTERN.matcher(text); matcher.find();) {
            keys.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
        for (Matcher matcher = URL_PATTERN.matcher(text); matcher.find();) {
            keys.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }

        return new ArrayList<>(keys);
    }
}
