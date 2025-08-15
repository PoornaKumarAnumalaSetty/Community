# Community Board - Senior-Friendly Edition

A simple and engaging community platform designed with older users in mind, featuring large text, high contrast, and easy navigation.

## Features

- **Senior-Friendly UI**: Large text, high contrast, simple navigation
- **Persistent Storage**: PostgreSQL database for all posts and comments
- **Easy Interaction**: Simple forms, clear buttons, readable fonts
- **Community Features**: Post issues/ideas, upvote, comment

## Requirements

- JDK 11+ installed (`java` and `javac` in PATH)
- PostgreSQL 12+ installed and running
- PostgreSQL JDBC driver (included)

## Database Setup

1. Install PostgreSQL from https://www.postgresql.org/download/
2. Create a database:
   ```sql
   CREATE DATABASE community_board;
   CREATE USER community_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE community_board TO community_user;
   ```
3. Update `database.properties` with your credentials

## Run (Windows PowerShell)

```powershell
cd Community
./run.ps1
```

Then open `http://localhost:8080` in your browser.

## Run (Command Prompt)

```bat
cd Community
run.bat
```

## Project Structure

- `src/` — Java source (HTTP server + PostgreSQL integration)
- `public/` — Frontend files (HTML/CSS/JS with senior-friendly design)
- `lib/` — PostgreSQL JDBC driver
- `out/` — Compiled classes (auto-created)
- `database.properties` — Database connection settings

## Notes

- All data is saved to PostgreSQL database
- Large, readable fonts and high contrast colors
- Simple, intuitive interface design
- Responsive design for different screen sizes
