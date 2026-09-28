@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

echo ========================================
echo XtoXray test environment
echo ========================================
echo.

set "PINECONE_ROOT=E:\Games\ElyPrismLauncher"
set "SOURCE_INSTANCE=%PINECONE_ROOT%\instances\Gregnautics"
set "TEST_INSTANCE=%PINECONE_ROOT%\instances\XtoXray_Test"
set "SOURCE_MC=%SOURCE_INSTANCE%\minecraft"
set "TEST_MC=%TEST_INSTANCE%\minecraft"
set "SOURCE_MODS=%SOURCE_MC%\mods"
set "TEST_MODS=%TEST_MC%\mods"
set "WORLD_NAME=XtoXray_TestWorld"
set "TEST_WORLD=%TEST_MC%\saves\%WORLD_NAME%"
set "LAUNCHER="

for %%L in (
    "%PINECONE_ROOT%\PineconeMC.exe"
    "%PINECONE_ROOT%\elyprismlauncher.exe"
    "%PINECONE_ROOT%\prismlauncher.exe"
    "%PINECONE_ROOT%\PrismLauncher.exe"
) do (
    if not defined LAUNCHER if exist "%%~L" set "LAUNCHER=%%~L"
)

if not defined LAUNCHER (
    echo [ERROR] PineconeMC/ElyPrism/Prism executable not found.
    echo Expected in:
    echo %PINECONE_ROOT%
    echo.
    pause
    exit /b 1
)

if not exist "%SOURCE_INSTANCE%\instance.cfg" (
    echo [ERROR] Source instance not found:
    echo %SOURCE_INSTANCE%
    echo.
    pause
    exit /b 1
)

if not exist "%SOURCE_INSTANCE%\mmc-pack.json" (
    echo [ERROR] Source instance metadata not found:
    echo %SOURCE_INSTANCE%\mmc-pack.json
    echo.
    pause
    exit /b 1
)

if not exist "%SOURCE_MC%" (
    echo [ERROR] Minecraft directory not found:
    echo %SOURCE_MC%
    echo.
    pause
    exit /b 1
)

echo [1/4] Building the mod...
call "%~dp0build.bat" /nopause
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

set "JAR="
for /f "delims=" %%J in ('dir /b /o-d "%~dp0build\libs\xtoxray-*.jar" 2^>nul') do (
    set "JAR=%~dp0build\libs\%%J"
    goto :jar_found
)

:jar_found
if not defined JAR (
    echo.
    echo [ERROR] XtoXray JAR was not found in build\libs.
    pause
    exit /b 1
)

echo.
echo [2/4] Preparing isolated test instance...

if not exist "%TEST_INSTANCE%" (
    mkdir "%TEST_INSTANCE%"
    if errorlevel 1 (
        echo [ERROR] Failed to create test instance.
        pause
        exit /b 1
    )
)

if not exist "%TEST_INSTANCE%\instance.cfg" (
    copy /y "%SOURCE_INSTANCE%\instance.cfg" "%TEST_INSTANCE%\instance.cfg" >nul
    powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Content -Raw '%TEST_INSTANCE%\instance.cfg') -replace '(?m)^name=.*$', 'name=XtoXray Test' | Set-Content -NoNewline '%TEST_INSTANCE%\instance.cfg'"
)

if not exist "%TEST_INSTANCE%\mmc-pack.json" (
    copy /y "%SOURCE_INSTANCE%\mmc-pack.json" "%TEST_INSTANCE%\mmc-pack.json" >nul
)

if not exist "%TEST_MC%" mkdir "%TEST_MC%"
if not exist "%TEST_MODS%" mkdir "%TEST_MODS%"
if not exist "%TEST_MC%\saves" mkdir "%TEST_MC%\saves"

if exist "%SOURCE_INSTANCE%\icon.png" if not exist "%TEST_INSTANCE%\icon.png" (
    copy /y "%SOURCE_INSTANCE%\icon.png" "%TEST_INSTANCE%\icon.png" >nul
)

if not exist "%TEST_MC%\options.txt" if exist "%SOURCE_MC%\options.txt" (
    copy /y "%SOURCE_MC%\options.txt" "%TEST_MC%\options.txt" >nul
)

for %%D in (config defaultconfigs kubejs scripts resourcepacks shaderpacks) do (
    if not exist "%TEST_MC%\%%D" if exist "%SOURCE_MC%\%%D" (
        robocopy "%SOURCE_MC%\%%D" "%TEST_MC%\%%D" /E /R:0 /W:0 /NFL /NDL /NJH /NJS >nul
    )
)

echo.
echo [3/4] Synchronizing mods and installing the fresh XtoXray build...
echo Advanced XRay is intentionally excluded from the test instance.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$src='%SOURCE_MODS%'; $dst='%TEST_MODS%';" ^
  "New-Item -ItemType Directory -Force -Path $dst | Out-Null;" ^
  "Get-ChildItem -LiteralPath $dst -Force | Remove-Item -Recurse -Force;" ^
  "Get-ChildItem -LiteralPath $src -File -Recurse | Where-Object { $_.Name -notmatch '^xtoxray-.*\.jar$' -and $_.Name -notmatch '^advanced-xray.*\.jar$' } | ForEach-Object {" ^
  "$rel=$_.FullName.Substring($src.Length).TrimStart('\');" ^
  "$target=Join-Path $dst $rel;" ^
  "New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null;" ^
  "New-Item -ItemType HardLink -Path $target -Target $_.FullName -ErrorAction Stop | Out-Null" ^
  "}"

if errorlevel 1 (
    echo [ERROR] Failed to synchronize mods.
    echo Make sure Minecraft and PineconeMC are closed.
    pause
    exit /b 1
)

copy /y "%JAR%" "%TEST_MODS%\" >nul
if errorlevel 1 (
    echo [ERROR] Failed to copy XtoXray JAR.
    pause
    exit /b 1
)

if not exist "%TEST_MC%\kubejs\server_scripts" (
    mkdir "%TEST_MC%\kubejs\server_scripts"
)

copy /y "%~dp0test-kubejs\xtoxray_test_lab.js" "%TEST_MC%\kubejs\server_scripts\xtoxray_test_lab.js" >nul
if errorlevel 1 (
    echo [ERROR] Failed to install the test-world script.
    pause
    exit /b 1
)

if not exist "%TEST_WORLD%\level.dat" (
    echo.
    echo ========================================
    echo FIRST RUN
    echo ========================================
    echo Test world "%WORLD_NAME%" does not exist yet.
    echo Launching the isolated instance now.
    echo.
    echo In Minecraft:
    echo 1. Create a Creative world named "%WORLD_NAME%".
    echo 2. Enter the world.
    echo 3. The test lab will be built automatically.
    echo.
    echo You can close the game and use test.bat again for later launches.
    echo ========================================
    echo.
) else (
    echo Test world found: %WORLD_NAME%
)

echo.
echo [4/4] Launching PineconeMC/ElyPrism test instance...
echo Instance: XtoXray_Test
echo.

"%LAUNCHER%" -d "%PINECONE_ROOT%" -l "XtoXray_Test"

echo.
echo Test launcher command finished.
pause
endlocal
