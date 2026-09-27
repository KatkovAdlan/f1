@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"

echo ========================================
echo X to Xray NeoForge - запуск Minecraft
echo ========================================
echo.

REM === Find Java ===
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found.
    echo.
    echo Установи Java 21 и повтори запуск.
    pause
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
    echo Скачиваю Gradle 9.2.1...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://services.gradle.org/distributions/gradle-9.2.1-bin.zip'; Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile 'gradle.zip'; Expand-Archive -Force 'gradle.zip' '.gradle-dist'; Remove-Item 'gradle.zip'"

    if errorlevel 1 (
        echo.
        echo [ERROR] Не удалось скачать или распаковать Gradle.
        pause
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
    pause
    exit /b 1
)

echo Gradle:
echo %GRADLE_HOME%
echo.
echo Запускаю Minecraft из NeoForge...
echo Первое включение может дополнительно скачать необходимые файлы.
echo.

call "%GRADLE_HOME%\bin\gradle.bat" runClient --no-daemon
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo ========================================
if "%EXIT_CODE%"=="0" (
    echo Minecraft завершён успешно.
) else (
    echo Запуск завершён с ошибкой.
)
echo Код завершения: %EXIT_CODE%
echo ========================================
echo.
pause
endlocal
