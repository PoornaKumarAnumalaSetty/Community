# 🚀 Community Board Project - Setup & Run Guide

## 📋 Prerequisites Check

Before running the project, ensure these are installed and running:

### 1. Check Java Installation

```powershell
java -version
```

**Expected Output**: Java 11+ (you have Java 23 ✅)

### 2. Check PostgreSQL Status

```powershell
netstat -an | findstr :5432
```

**Expected Output**: Should show PostgreSQL listening on port 5432

### 3. Check Database Configuration

Verify `database.properties` has correct credentials:

```properties
db.url=jdbc:postgresql://localhost:5432/community_board
db.user=postgres
db.password=Setty@123
db.driver=org.postgresql.Driver
```

## 🏃‍♂️ Running the Project

### Option 1: Using PowerShell Script (Recommended)

```powershell
cd C:\Users\aspk1\OneDrive\Desktop\Community
.\run.ps1
```

### Option 2: Manual Commands

```powershell
# Navigate to project directory
cd C:\Users\aspk1\OneDrive\Desktop\Community

# Create output directory if it doesn't exist
if (!(Test-Path out)) { New-Item -ItemType Directory -Path out | Out-Null }

# Compile Java source
javac -cp "lib/*" -d out -encoding UTF-8 src\*.java

# Run the application
java -cp "out;lib/*" Main
```

### Option 3: Using Batch File

```cmd
cd C:\Users\aspk1\OneDrive\Desktop\Community
run.bat
```

## ✅ Verification Commands

### Check if Server is Running

```powershell
netstat -an | findstr :8080
```

**Expected Output**: Should show port 8080 listening

### Test Server Response

```powershell
Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing | Select-Object StatusCode, StatusDescription
```

**Expected Output**: StatusCode 200, StatusDescription OK

### Check Database Connection

```powershell
# The server will show database connection status when starting
# Look for: "✅ Database connected successfully"
```

## 🌐 Access the Application

Once running, open your web browser and navigate to:
**http://localhost:8080**

## 🛑 Stopping the Server

### Method 1: Keyboard Interrupt

Press `Ctrl+C` in the terminal where the server is running

### Method 2: Close Terminal

Close the PowerShell/Command Prompt window

### Method 3: Kill Process (if needed)

```powershell
# Find Java process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

## 🔧 Troubleshooting

### Port Already in Use Error

If you get "Address already in use: bind":

```powershell
# Check what's using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID from above command)
taskkill /PID <PID> /F

# Verify port is free
netstat -an | findstr :8080

# Now try running the project again
.\run.ps1
```

**Example of resolving the error:**

```powershell
# 1. Check what's using port 8080
netstat -ano | findstr :8080
# Output: TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       17240

# 2. Kill the process (PID 17240 in this example)
taskkill /PID 17240 /F

# 3. Verify port is free
netstat -an | findstr :8080

# 4. Run the project
.\run.ps1
```

### Database Connection Issues

1. Ensure PostgreSQL is running
2. Verify database credentials in `database.properties`
3. Check if database `community_board` exists
4. Ensure user has proper permissions

### Compilation Issues

1. Verify Java is in PATH
2. Check if `lib/postgresql-42.7.2.jar` exists
3. Ensure source files are in `src/` directory

## 📁 Project Structure

```
Community/
├── src/                    # Java source files
├── public/                 # Frontend files (HTML/CSS/JS)
├── lib/                    # PostgreSQL JDBC driver
├── out/                    # Compiled classes (auto-created)
├── database.properties     # Database configuration
├── run.ps1                # PowerShell run script
├── run.bat                # Batch run script
└── README.md              # Project documentation
```

## 🎯 Quick Start (Copy & Paste)

```powershell
# Navigate to project
cd C:\Users\aspk1\OneDrive\Desktop\Community

# Run the project
.\run.ps1

# Verify it's running
netstat -an | findstr :8080

# Open in browser: http://localhost:8080
```

## 📝 Notes

- Server runs on port 8080 by default
- Database must be running before starting the application
- All data is persisted in PostgreSQL
- Application features senior-friendly UI design
- Server runs in foreground by default (use Ctrl+C to stop)
