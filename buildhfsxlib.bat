@echo off
:begin
setlocal
pushd %~dp0

set SOURCES_DIR=%~dp0src
set CLASSFILES_DIR=%~dp0build.~
set BUILD_LIB_PATH=%~dp0lib
set DIST_LIB_PATH=%~dp0dist\lib
set JARFILE=hfsx_dmglib.jar
set MANIFEST=%~dp0meta\manifest.txt
set BUILD_CP="%DIST_LIB_PATH%\apache-ant-1.7.0-bzip2.jar";"%DIST_LIB_PATH%\iharder-base64.jar"

echo Removing all class files...
if exist "%CLASSFILES_DIR%" rmdir /s /q %CLASSFILES_DIR%
mkdir "%CLASSFILES_DIR%"

echo Compiling org.catacombae.udif.UDIFRandomAccessStream (and dependencies)...
javac -cp %BUILD_CP% -sourcepath "%SOURCES_DIR%" -d "%CLASSFILES_DIR%" -Xlint:unchecked "%SOURCES_DIR%\org\catacombae\udif\UDIFRandomAccessStream.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file...
if not exist "%BUILD_LIB_PATH%" mkdir "%BUILD_LIB_PATH%"
jar cvf "%BUILD_LIB_PATH%\%JARFILE%" -C "%CLASSFILES_DIR%" . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done! Remember to include dependencies %BUILD_CP% in target.) else echo Problems while building jar-file...

goto end

:error
echo There were errors...
goto end

:end
popd
endlocal
