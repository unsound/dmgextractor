@echo off
:begin
setlocal

set SOURCES_DIR=src
set CLASSFILES_DIR=build.~
set LIBRARY_PATH=lib
set JARFILE=dmgextractor.jar
set MANIFEST=meta\metafile.txt

pushd %~dp0
echo Removing all class files...
if not exist %CLASSFILES_DIR% mkdir %CLASSFILES_DIR%
del /f /q %CLASSFILES_DIR%\*.*
echo Incrementing build number...
java -cp .\buildenumerator BuildEnumerator src\BuildNumber.java
echo Compiling...
javac -sourcepath %SOURCES_DIR% -d %CLASSFILES_DIR% -Xlint:unchecked %SOURCES_DIR%\*.java
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error
echo Building jar-file...
if not exist %LIBRARY_PATH% mkdir %LIBRARY_PATH%
jar cvfm %LIBRARY_PATH%\%JARFILE% %MANIFEST% -C %CLASSFILES_DIR% . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done!) else echo Problems while building jar-file...
popd
goto end

:error
echo There were errors...
goto end

:end
endlocal
