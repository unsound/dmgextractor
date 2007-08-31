@echo off
:begin
setlocal

set SOURCES_DIR=src
set CLASSFILES_DIR=build.~
set LIBRARY_PATH=lib
set JARFILE=hfsx_dmglib.jar
set MANIFEST=meta\manifest.txt
set BUILD_CP=%LIBRARY_PATH%\apache-ant-1.7.0-bzip2.jar;%LIBRARY_PATH%\iharder-base64.jar

pushd %~dp0

echo Removing all class files...
if exist %CLASSFILES_DIR% rmdir /s /q %CLASSFILES_DIR%
mkdir %CLASSFILES_DIR%

echo Compiling org.catacombae.udif.UDIFRandomAccessStream (and dependencies)...
javac -cp %BUILD_CP% -sourcepath %SOURCES_DIR% -d %CLASSFILES_DIR% -Xlint:unchecked %SOURCES_DIR%\org\catacombae\udif\UDIFRandomAccessStream.java
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file...
if not exist %LIBRARY_PATH% mkdir %LIBRARY_PATH%
jar cvf %LIBRARY_PATH%\%JARFILE% -C %CLASSFILES_DIR% . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done! Remember to include dependencies %BUILD_CP% in target.) else echo Problems while building jar-file...

popd
goto end

:error
echo There were errors...
goto end

:end
endlocal
