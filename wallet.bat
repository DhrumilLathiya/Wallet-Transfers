@echo off
set "JAR_PATH=%~dp0target\wallet-transfers-1.0.0.jar"
if not exist "%JAR_PATH%" (
    echo Building project...
    call "%~dp0mvnw.cmd" package -DskipTests
)
java -jar "%JAR_PATH%" %*
