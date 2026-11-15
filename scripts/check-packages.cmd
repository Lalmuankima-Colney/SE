@echo off
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" scripts\CheckPackages.java %*
) else (
    java scripts\CheckPackages.java %*
)
