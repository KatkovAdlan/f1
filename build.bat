@echo off
setlocal

cd /d "%~dp0"

echo ========================================
echo X to Xray NeoForge build
echo ========================================
echo.

REM === Find Java ===
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found.
    if /I not "%~1"=="/nopause" pause
    exit /b 1
)

for /f "delims=" %%J in ('where java') do (
    set "JAVA_EXE=%%J"
    goto :java_found
)

:java_found

for %%J in ("%JAVA_EXE%") do set "JAVA_BIN=%%~dpJ"
for %%J in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fJ"

echo Java executable:
echo %JAVA_EXE%
echo.
echo JAVA_HOME:
echo %JAVA_HOME%
echo.

java -version
echo.

REM === Download Gradle if necessary ===
if not exist ".gradle-dist" (
    echo Downloading Gradle 9.2.1...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-9.2.1-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile 'gradle.zip'; Expand-Archive -Force 'gradle.zip' '.gradle-dist'; Remove-Item 'gradle.zip'"

    if errorlevel 1 (
        echo.
        echo [ERROR] Failed to download or extract Gradle.
        if /I not "%~1"=="/nopause" pause
        exit /b 1
    )
)

set "GRADLE_HOME="

for /d %%G in (".gradle-dist\gradle-*") do (
    set "GRADLE_HOME=%%~fG"
)

if not defined GRADLE_HOME (
    echo.
    echo [ERROR] Gradle not found.
    if /I not "%~1"=="/nopause" pause
    exit /b 1
)

echo Gradle:
echo %GRADLE_HOME%
echo.

call "%GRADLE_HOME%\bin\gradle.bat" build
set "BUILD_EXIT=%ERRORLEVEL%"

echo.
echo ========================================
echo Build exit code: %BUILD_EXIT%
echo ========================================
echo.

if exist "build\libs" (
    echo JAR files:
    dir "build\libs"
) else (
    echo [WARNING] build\libs was not created.
)

if /I not "%~1"=="/nopause" (
    echo.
    pause
)

exit /b %BUILD_EXIT%
