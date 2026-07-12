# Deploy lên Heroku

## Bước 1: Cài đặt Heroku CLI
Download từ: https://devcenter.heroku.com/articles/heroku-cli

```bash
# Verify installation
heroku --version
```

## Bước 2: Login vào Heroku
```bash
heroku login
```

## Bước 3: Tạo Heroku app
```bash
heroku create your-app-name
# Ví dụ: heroku create github-jira-webhook-prod
```

## Bước 4: Set environment variables
```bash
heroku config:set JIRA_BASE_URL=https://chautminhtran.atlassian.net
heroku config:set JIRA_BOT_EMAIL=chautminhtran@gmail.com
heroku config:set JIRA_API_TOKEN=your_api_token_here
heroku config:set GITHUB_WEBHOOK_SECRET=25102003
heroku config:set JIRA_TRANSITION_MAIN=41
heroku config:set JIRA_TRANSITION_DEVELOP=42
heroku config:set JIRA_TRANSITION_RELEASE=42
heroku config:set JIRA_TRANSITION_DEFAULT=42
```

## Bước 5: Deploy
```bash
git push heroku main
# hoặc nếu deploy từ feature branch:
# git push heroku feature/github-jira-webhook:main
```

## Bước 6: Cấu hình GitHub Webhook
Vào repo → Settings → Webhooks → Add webhook:

- **Payload URL**: `https://your-app-name.herokuapp.com/github/pr`
- **Content type**: `application/json`
- **Secret**: `25102003`
- **Which events?**: Pull request → Closed
- ✅ Active

## Bước 7: Test webhook
Tạo một PR, merge nó vào main, rồi kiểm tra:
- Jira ticket có thay đổi status không?
- Xem logs: `heroku logs --tail`

## Debug
```bash
# Xem logs real-time
heroku logs --tail

# Xem config variables
heroku config

# Restart app
heroku restart

# Xem status
heroku ps
```

## Rollback nếu có vấn đề
```bash
heroku releases
heroku rollback
```
