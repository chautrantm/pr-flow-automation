# Implementation notes

This repository now contains a minimal Java webhook service that can receive GitHub pull request webhooks and transition Jira issues when a PR is merged.

## Files added
- [pom.xml](pom.xml)
- [src/main/java/com/example/prflow/JiraKeyExtractor.java](src/main/java/com/example/prflow/JiraKeyExtractor.java)
- [src/main/java/com/example/prflow/JiraTransitionService.java](src/main/java/com/example/prflow/JiraTransitionService.java)
- [src/main/java/com/example/prflow/GithubPrWebhookServer.java](src/main/java/com/example/prflow/GithubPrWebhookServer.java)
- [src/test/java/com/example/prflow/JiraKeyExtractorTest.java](src/test/java/com/example/prflow/JiraKeyExtractorTest.java)

## How it works
1. GitHub sends a PR webhook when the PR is closed.
2. The server extracts Jira keys from the PR title, body, and branch name.
3. It decides which Jira transition to use by the target branch (`develop`, `main`, or default).
4. It calls Jira REST API to transition each found issue.

## Required environment variables
- `JIRA_BASE_URL`
- `JIRA_BOT_EMAIL`
- `JIRA_API_TOKEN`

## Important caveat
Jira needs a transition ID, not a status name. Ask your Jira Admin for the exact transition IDs for the workflow you want.
