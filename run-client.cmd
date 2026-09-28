@echo off
setlocal

cd /d "%~dp0"

echo ========================================
echo X to Xray NeoForge - Minecraft launcher
echo ========================================
echo.
echo Working directory:
echo %CD%
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java was not found.
    echo Java 21 is required.
    echo.
    pause
    exit /b 1
)

for /f "delims=" %%J in ('where java') do (
    set "JAVA_EXE=%%J"
    goto java_found
)

:java_found
for %%J in ("%JAVA_EXE%") do set "JAVA_BIN=%%~dpJ"
for %%J in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fJ"

echo Java:
echo %JAVA_EXE%
echo.
java -version
echo.

if not exist ".gradle-dist" (
    echo Downloading Gradle 9.2.1...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-9.2.1-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile 'gradle.zip'; Expand-Archive -Force 'gradle.zip' '.gradle-dist'; Remove-Item 'gradle.zip'"

    if errorlevel 1 (
        echo.
        echo ERROR: Gradle download or extraction failed.
        echo.
        pause
        exit /b 1
    )
)

set "GRADLE_HOME="
for /d %%G in (".gradle-dist\gradle-*") do set "GRADLE_HOME=%%~fG"

if not defined GRADLE_HOME (
    echo.
    echo ERROR: Gradle was not found.
    echo.
    pause
    exit /b 1
)

echo Gradle:
echo %GRADLE_HOME%
echo.
echo Starting NeoForge Minecraft...
echo.

call "%GRADLE_HOME%\bin\gradle.bat" runClient --no-daemon
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo ========================================
echo Launcher finished. Exit code: %EXIT_CODE%
echo ========================================
echo.
pause
endlocal
