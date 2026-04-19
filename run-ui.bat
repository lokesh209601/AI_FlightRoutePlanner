@echo off
setlocal

set "JAVAFX_HOME_VALUE=%JAVAFX_HOME%"
if "%JAVAFX_HOME_VALUE%"=="" set "JAVAFX_HOME_VALUE=C:\Program Files\javafx-sdk-24.0.1"
set "JAVAFX_LIB=%JAVAFX_HOME_VALUE%\lib"

if not exist "%JAVAFX_LIB%" (
    echo JavaFX SDK lib folder not found at "%JAVAFX_LIB%".
    echo Set JAVAFX_HOME to your JavaFX SDK directory and try again.
    exit /b 1
)

javac src\*.java
if errorlevel 1 exit /b %errorlevel%

java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.swing,javafx.web,jdk.httpserver --enable-native-access=javafx.graphics,javafx.web --sun-misc-unsafe-memory-access=allow -cp src A1Main
