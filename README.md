# PR Flow Automation

MVP webhook nhận GitHub pull request webhook và tự động transition Jira issue khi PR được merge.

## What this does
- Extracts Jira keys from PR title/body/branch name using regex ([A-Z][A-Z0-9_]+-\d+)
- Maps target branch to Jira transition ID
- Calls Jira REST API to transition the issue

## Required environment variables
- JIRA_BASE_URL -> e.g. https://your-company.atlassian.net
- JIRA_BOT_EMAIL -> service account email
- JIRA_API_TOKEN -> Atlassian API token
- GITHUB_WEBHOOK_SECRET -> optional secret for signature validation

## Example branch mapping
You can change these in the code or pass them in later.
- develop -> 31
- main -> 41
- default -> 51

## Run locally
1. Copy the example env file and fill in your values:

   Copy-Item .env.example .env
   # then edit .env with your Jira/GitHub values

2. Start the server from PowerShell:

   .\run-local.ps1

3. The server listens on:

   http://localhost:8080/github/pr

4. To test locally without a GitHub webhook, send a POST request:

   curl -X POST http://localhost:8080/github/pr \
     -H "Content-Type: application/json" \
     -d @payload.json

   If you set `GITHUB_WEBHOOK_SECRET`, send the correct `X-Hub-Signature-256` header.
   If you do not set `GITHUB_WEBHOOK_SECRET`, the server will accept requests without signature validation.

## GitHub webhook setup
- Payload URL: https://your-domain/github/pr
- Content type: pplication/json
- Trigger: Pull request
- Event: closed

## Important Jira notes
- Jira requires transition ID, not status name.
- The bot account must have permission to transition issues.
- Ask Jira Admin for the exact transition IDs for your workflow.
- If your workflow requires multiple steps, configure them explicitly.

## Example PR title
- [ABC-123] Fix login API

## Notes for your request
If the PR description contains a Jira story link such as:
https://chautminhtran.atlassian.net/browse/SCRUM-2
this MVP will extract SCRUM-2 from the description if it is present as plain text or in the title/body/branch.
