$ErrorActionPreference = 'Stop'

$javaFxHome = $env:JAVAFX_HOME
if (-not $javaFxHome) {
    $javaFxHome = 'C:\Program Files\javafx-sdk-24.0.1'
}

$javaFxLib = Join-Path $javaFxHome 'lib'
if (-not (Test-Path $javaFxLib)) {
    throw "JavaFX SDK lib folder not found at '$javaFxLib'. Set JAVAFX_HOME to your JavaFX SDK directory."
}

javac src\*.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java --module-path "$javaFxLib" --add-modules javafx.controls,javafx.swing,javafx.web,jdk.httpserver --enable-native-access=javafx.graphics,javafx.web --sun-misc-unsafe-memory-access=allow -cp src A1Main
