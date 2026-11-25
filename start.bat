@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Atlas Client - Build mod jar

echo.
echo  Atlas Client - Minecraft 1.21.11
 echo -----------------------------------
echo  Preparing Java 21 and building the mod...
echo.

set "JAVA_HOME="
for /f "usebackq delims=" %%J in (`powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\ensure-java.ps1"`) do set "JAVA_HOME=%%J"
if not defined JAVA_HOME (
    echo.
    echo Could not prepare Java 21. See the error above.
    goto :failed
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java was not found at "%JAVA_HOME%".
    goto :failed
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using Java from: %JAVA_HOME%
echo Gradle will download its build tools and dependencies if needed.
echo.
call "%~dp0gradlew.bat" --no-daemon clean build
if errorlevel 1 (
    echo.
    echo Build failed. The Gradle output above should show the cause.
    goto :failed
)

set "BUILT_JAR=%~dp0build\libs\atlas-client-1.0.0.jar"
if not exist "%BUILT_JAR%" (
    echo.
    echo Build completed, but the expected jar was not found:
    echo %BUILT_JAR%
    goto :failed
)
if not exist "%~dp0dist" mkdir "%~dp0dist"
copy /y "%BUILT_JAR%" "%~dp0dist\AtlasClient-1.0.0.jar" >nul
if errorlevel 1 (
    echo Could not copy the jar to the dist folder.
    goto :failed
)

echo.
echo Build successful!
echo Your jar is ready at:
echo %~dp0dist\AtlasClient-1.0.0.jar
echo.
echo Copy that jar into your Minecraft 1.21.11 Fabric mods folder.
echo.
pause
exit /b 0

:failed
echo.
pause
exit /b 1
