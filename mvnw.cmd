@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "WRAPPER_DIR=%~dp0"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIST_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%-bin"
set "MAVEN_HOME=%MAVEN_DIST_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if exist "%MAVEN_CMD%" goto run

echo Installing Maven %MAVEN_VERSION% wrapper...
if not exist "%MAVEN_DIST_DIR%" mkdir "%MAVEN_DIST_DIR%"

set "ZIP_PATH=%MAVEN_DIST_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%ZIP_PATH%'); Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%MAVEN_DIST_DIR%' -Force; Remove-Item '%ZIP_PATH%'"

if not exist "%MAVEN_CMD%" (
    echo Error: Failed to install Maven %MAVEN_VERSION%
    exit /b 1
)

:run
call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
