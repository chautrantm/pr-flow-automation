@echo off
REM Script to check available transitions for a Jira issue

set JIRA_BASE_URL=https://chautminhtran.atlassian.net
set JIRA_BOT_EMAIL=chautminhtran@gmail.com
set JIRA_API_TOKEN=JATATT3xFfGF0f24812mrFS5OFWTN9P7ldALSWt9leCGfM8CFrJRe_9_PyihhKvKpl8IgKm8D-yI--T9GUnsUKh3oq48CLv0tb8Z6HXvWyTy9aGAeJ8gOCErA5OFp7iNUimPqQcaQpL5CqX4GCdad5WFqoxJR7kTl6TIdprHPQxGlTXzRgTuFz80=FCB31009

REM Replace SCRUM-2 with your actual issue key
set ISSUE_KEY=SCRUM-2

echo Checking transitions for issue: %ISSUE_KEY%
echo.

curl.exe -u "%JIRA_BOT_EMAIL%:%JIRA_API_TOKEN%" ^
  -X GET ^
  "%JIRA_BASE_URL%/rest/api/3/issue/%ISSUE_KEY%/transitions" ^
  -H "Accept: application/json"

echo.
echo.
echo Check the output above. You'll see:
echo {
echo   "transitions": [
echo     {
echo       "id": "11",
echo       "name": "In Progress",
echo       ...
echo     },
echo     {
echo       "id": "41", 
echo       "name": "Done",
echo       ...
echo     }
echo   ]
echo }
echo.
echo Update .env with the correct transition IDs
pause
