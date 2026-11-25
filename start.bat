@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Atlas Client - Build mod jar

echo.
echo  Atlas Client - Minecraft 1.21.11
echo  -----------------------------------
echo  Preparing source, Java 25, and the mod build...
echo.

set "PROJECT_DIR=%~dp0"
set "STANDALONE=0"
if exist "%PROJECT_DIR%build.gradle" if exist "%PROJECT_DIR%gradlew.bat" goto :project_ready

set "STANDALONE=1"
echo This is the standalone launcher. Preparing the full project source...
set "ATLAS_WORK=%USERPROFILE%\AtlasClientBuild"
set "ATLAS_SOURCE=%ATLAS_WORK%\source"
set "ATLAS_ZIP=%ATLAS_WORK%\source.zip"
set "ATLAS_PROJECT_FILE=%ATLAS_WORK%\project-path.txt"
if not exist "%ATLAS_WORK%" mkdir "%ATLAS_WORK%"

if exist "%ATLAS_PROJECT_FILE%" (
    set /p "PROJECT_DIR="<"%ATLAS_PROJECT_FILE%"
    call :check_cached_project
    if not errorlevel 1 goto :project_ready
)
if exist "%ATLAS_PROJECT_FILE%" del /q "%ATLAS_PROJECT_FILE%"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $root=$env:ATLAS_SOURCE; if(Test-Path $root){Remove-Item -LiteralPath $root -Recurse -Force}; Invoke-WebRequest -Uri 'https://github.com/denightlocator/alatar/archive/refs/heads/arena/32825206-alatar.zip' -OutFile $env:ATLAS_ZIP -UseBasicParsing | Out-Null; Expand-Archive -LiteralPath $env:ATLAS_ZIP -DestinationPath $root -Force | Out-Null; $f=Get-ChildItem -LiteralPath $root -Filter 'build.gradle' -File -Recurse | Where-Object { Test-Path (Join-Path $_.DirectoryName 'gradlew.bat') } | Select-Object -First 1; if(!$f){throw 'Could not find the Atlas Gradle project in the downloaded source.'}; [System.IO.File]::WriteAllText($env:ATLAS_PROJECT_FILE,$f.DirectoryName)"
if errorlevel 1 (
    echo.
    echo Could not download the source. Check your internet connection and try again.
    goto :failed
)
if not exist "%ATLAS_PROJECT_FILE%" (
    echo The source download did not return a project folder.
    goto :failed
)
set /p "PROJECT_DIR="<"%ATLAS_PROJECT_FILE%"
if not defined PROJECT_DIR (
    echo The downloaded project path is empty.
    goto :failed
)

goto :project_ready

:check_cached_project
if not exist "%PROJECT_DIR%\build.gradle" exit /b 1
if not exist "%PROJECT_DIR%\gradlew.bat" exit /b 1
exit /b 0

:project_ready
if not "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR%\"
if not exist "%PROJECT_DIR%scripts\ensure-java.ps1" (
    echo Missing Java bootstrap script: "%PROJECT_DIR%scripts\ensure-java.ps1"
    goto :failed
)
if "%STANDALONE%"=="1" (
    echo Updating the Java setup helper...
    set "ATLAS_PROJECT_DIR=%PROJECT_DIR%"
    powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/denightlocator/alatar/arena/32825206-alatar/scripts/ensure-java.ps1' -OutFile (Join-Path $env:ATLAS_PROJECT_DIR 'scripts\ensure-java.ps1') -UseBasicParsing | Out-Null"
    if errorlevel 1 (
        echo Could not update the Java helper. Check your internet connection and try again.
        goto :failed
    )
)
if not exist "%PROJECT_DIR%.tools" mkdir "%PROJECT_DIR%.tools"
if exist "%PROJECT_DIR%.tools\java-home.txt" del /q "%PROJECT_DIR%.tools\java-home.txt"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%scripts\ensure-java.ps1"
if errorlevel 1 (
    echo.
    echo Could not prepare Java 25. See the error above.
    goto :failed
)
if not exist "%PROJECT_DIR%.tools\java-home.txt" (
    echo The Java setup did not return a JDK path.
    goto :failed
)
set "JAVA_HOME="
set /p "JAVA_HOME="<"%PROJECT_DIR%.tools\java-home.txt"
if not defined JAVA_HOME (
    echo The Java setup returned an empty JDK path.
    goto :failed
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java was not found at "%JAVA_HOME%".
    echo Delete "%PROJECT_DIR%.tools\java-home.txt" and try again.
    goto :failed
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%PROJECT_DIR%"
echo.
echo Using Java from: "%JAVA_HOME%"
echo Building the Fabric mod. Gradle downloads build dependencies if needed.
echo.
call "%PROJECT_DIR%gradlew.bat" --no-daemon clean build
if errorlevel 1 (
    echo.
    echo Build failed. The Gradle output above should show the cause.
    goto :failed
)

set "BUILT_JAR=%PROJECT_DIR%build\libs\atlas-client-1.0.0.jar"
if not exist "%BUILT_JAR%" (
    echo.
    echo Build completed, but the expected jar was not found:
    echo %BUILT_JAR%
    goto :failed
)
if not exist "%PROJECT_DIR%dist" mkdir "%PROJECT_DIR%dist"
copy /y "%BUILT_JAR%" "%PROJECT_DIR%dist\AtlasClient-1.0.0.jar" >nul
if errorlevel 1 (
    echo Could not copy the jar to the dist folder.
    goto :failed
)

echo.
echo Build successful!
echo Your jar is ready at:
echo %PROJECT_DIR%dist\AtlasClient-1.0.0.jar
echo.
echo Copy that jar into your Minecraft 1.21.11 Fabric mods folder.
echo.
pause
exit /b 0

:failed
echo.
pause
exit /b 1
