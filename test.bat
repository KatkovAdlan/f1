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
set "LAUNCHER=%PINECONE_ROOT%\elyprismlauncher.exe"
set "JAR="

if not exist "%LAUNCHER%" (
    if exist "%PINECONE_ROOT%\prismlauncher.exe" set "LAUNCHER=%PINECONE_ROOT%\prismlauncher.exe"
)

if not exist "%LAUNCHER%" (
    echo [ERROR] ElyPrism/PineconeMC executable not found:
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

echo [1/4] Building the mod...
call "%~dp0build.bat"
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed.
    exit /b 1
)

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

    copy /y "%SOURCE_INSTANCE%\instance.cfg" "%TEST_INSTANCE%\instance.cfg" >nul
    copy /y "%SOURCE_INSTANCE%\mmc-pack.json" "%TEST_INSTANCE%\mmc-pack.json" >nul

    if exist "%SOURCE_INSTANCE%\icon.png" copy /y "%SOURCE_INSTANCE%\icon.png" "%TEST_INSTANCE%\icon.png" >nul

    mkdir "%TEST_MC%"
    mkdir "%TEST_MODS%"
    mkdir "%TEST_MC%\saves"

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$src='%SOURCE_MC%'; $dst='%TEST_MC%';" ^
      "Get-ChildItem -LiteralPath $src -File -Force | Where-Object { $_.Name -notin @('servers.dat','servers.dat_old') } | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $dst $_.Name) -Force }"

    for %%D in (config defaultconfigs kubejs scripts resourcepacks shaderpacks) do (
        if exist "%SOURCE_MC%\%%D" (
            robocopy "%SOURCE_MC%\%%D" "%TEST_MC%\%%D" /E /R:0 /W:0 /NFL /NDL /NJH /NJS >nul
        )
    )

    if exist "%SOURCE_MC%\options.txt" copy /y "%SOURCE_MC%\options.txt" "%TEST_MC%\options.txt" >nul

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$src='%SOURCE_MODS%'; $dst='%TEST_MODS%';" ^
      "Get-ChildItem -LiteralPath $src -File -Recurse | Where-Object { $_.Name -notlike 'xtoxray-*.jar' } | ForEach-Object {" ^
      "$rel=$_.FullName.Substring($src.Length).TrimStart('\');" ^
      "$target=Join-Path $dst $rel;" ^
      "New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null;" ^
      "New-Item -ItemType HardLink -Path $target -Target $_.FullName -ErrorAction Stop | Out-Null" ^
      "}"

    echo Test instance created:
    echo %TEST_INSTANCE%
) else (
    echo Test instance already exists. Keeping it.
)

echo.
echo [3/4] Installing the freshly built XtoXray JAR...

if not exist "%TEST_MODS%" mkdir "%TEST_MODS%"

del /q "%TEST_MODS%\xtoxray-*.jar" >nul 2>nul
copy /y "%JAR%" "%TEST_MODS%\" >nul

if errorlevel 1 (
    echo [ERROR] Failed to copy XtoXray JAR.
    pause
    exit /b 1
)

if exist "%TEST_WORLD%\level.dat" (
    echo Test world found: %WORLD_NAME%
    if exist "%~dp0test-datapack" (
        robocopy "%~dp0test-datapack" "%TEST_WORLD%\datapacks\xtoxray-test" /E /R:0 /W:0 /NFL /NDL /NJH /NJS >nul
    )
) else (
    echo.
    echo Test world "%WORLD_NAME%" has not been created yet.
    echo First launch will open the isolated test instance.
    echo Create a world named "%WORLD_NAME%" once, then run this BAT again.
)

echo.
echo [4/4] Launching PineconeMC/ElyPrism test instance...
echo Instance ID: XtoXray_Test
echo.

"%LAUNCHER%" -d "%PINECONE_ROOT%" -l "XtoXray_Test"

echo.
echo PineconeMC launch command finished.
pause
endlocal
