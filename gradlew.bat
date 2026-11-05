@rem
@rem  Gradle start up script for Windows
@rem
@if "%DEBUG%"=="" @echo off
@if "%JAVA_HOME%"=="" goto noJavaHome

set JAVA_EXE=%JAVA_HOME%\bin\java.exe
goto runGradle

:noJavaHome
set JAVA_EXE=java.exe

:runGradle
set DIRNAME=%~dp0
if exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
    "%JAVA_EXE%" -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
    gradle %*
)
