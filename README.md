## Community Board (Java + HTML/CSS/JS)

Simple and engaging community page to post issues/ideas, upvote them, and add comments.

### Requirements
- JDK 11+ installed (`java` and `javac` in PATH)

### Run (Windows PowerShell)
```powershell
cd Community
./run.ps1
```
Then open `http://localhost:8080` in your browser.

### Run (Command Prompt)
```bat
cd Community
run.bat
```

### Project Structure
- `src/` — Java source (embedded HTTP server and in-memory store)
- `public/` — Frontend files (HTML/CSS/JS)
- `out/` — Compiled classes (auto-created)

### Notes
- Data is in-memory only. Restarting the server resets all issues.
- Endpoints:
  - `GET /api/issues` — List issues
  - `POST /api/issues` — Create issue (form-encoded: `title`, `description`)
  - `POST /api/issues/{id}/vote` — Upvote issue
  - `POST /api/issues/{id}/comments` — Add comment (form-encoded: `text`)


