@echo off
setlocal
set "APP_HOME=%~dp0"
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
if exist "%WRAPPER_JAR%" (
  java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)
set "GRADLE_VERSION=8.7"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "CACHE_DIR=%GRADLE_USER_HOME%\wrapper\dists\gooskers-bootstrap\gradle-%GRADLE_VERSION%"
set "DIST=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_HOME=%CACHE_DIR%\gradle-%GRADLE_VERSION%"
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%DIST%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST%'"
    if errorlevel 1 exit /b %ERRORLEVEL%
  )
  if exist "%GRADLE_HOME%" rmdir /s /q "%GRADLE_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%DIST%' -DestinationPath '%CACHE_DIR%' -Force"
  if errorlevel 1 exit /b %ERRORLEVEL%
)
call "%GRADLE_HOME%\bin\gradle.bat" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
