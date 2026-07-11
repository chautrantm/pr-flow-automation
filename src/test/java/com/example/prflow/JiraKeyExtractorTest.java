package com.example.prflow;

import java.util.List;

public class JiraKeyExtractorTest {
    public static void main(String[] args) {
        JiraKeyExtractor extractor = new JiraKeyExtractor();

        List<String> keysFromTitleAndBranch = extractor.extract("[ABC-123] Fix login API for feature/ABC-123-login");
        assertEquals(List.of("ABC-123"), keysFromTitleAndBranch, "Should extract key from title and branch");

        List<String> keysFromUrl = extractor.extract("Please update https://example.atlassian.net/browse/SCRUM-2");
        assertEquals(List.of("SCRUM-2"), keysFromUrl, "Should extract key from Jira URL");

        List<String> empty = extractor.extract("No ticket in this text");
        assertEquals(List.of(), empty, "Should return empty list when no key exists");

        System.out.println("All Jira key extractor tests passed");
    }

    private static void assertEquals(List<String> expected, List<String> actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " Expected=" + expected + " Actual=" + actual);
        }
    }
}
