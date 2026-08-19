@echo off
echo ===================================================
echo  AgroAssist E2E Framework - Automated Git & GitHub Push
echo ===================================================

SET GIT_PATH="C:\Program Files\Git\cmd\git.exe"

if exist %GIT_PATH% (
    SET GIT_CMD=%GIT_PATH%
) else (
    SET GIT_CMD=git
)

echo [1/4] Initializing Git Repository...
%GIT_CMD% init

echo [2/4] Staging All Framework Files...
%GIT_CMD% add .

echo [3/4] Creating Commit...
%GIT_CMD% commit -m "Add complete E2E QA Automation Testing Framework"

echo ===================================================
echo  Git commit complete!
echo  To push to your remote GitHub repository, run:
echo  %GIT_CMD% remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
echo  %GIT_CMD% branch -M main
echo  %GIT_CMD% push -u origin main
echo ===================================================
pause
