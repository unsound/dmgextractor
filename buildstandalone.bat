@echo off
:begin
setlocal

set SOURCES_DIR=%~dp0src
set CLASSFILES_DIR=build.~
set BUILD_LIB_PATH=%~dp0lib
set DIST_LIB_PATH=%~dp0dist\lib
set JARFILE=%BUILD_LIB_PATH%\dmgextractor-standalone.jar
set MANIFEST=%~dp0doc\standalone-manifest\manifest.txt
set BUILD_CP=%CLASSFILES_DIR%
set BUILDTOOLS_CP=%BUILD_LIB_PATH%\buildenumerator.jar

pushd %~dp0

echo Removing all class files...
if exist %CLASSFILES_DIR% rmdir /s /q %CLASSFILES_DIR%
mkdir %CLASSFILES_DIR%

echo Extracting Apache bzip2 libraries to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\apache-ant-1.7.0-bzip2.jar"
popd

echo Extracting iharder-base64 to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\iharder-base64.jar"
popd

echo Compiling org.catacombae.dmgx.DMGExtractorGraphical and dependencies...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\dmgx\DMGExtractorGraphical.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file...
if not exist "%BUILD_LIB_PATH%" mkdir "%BUILD_LIB_PATH%"
jar cvfm "%JARFILE%" "%MANIFEST%" -C "%CLASSFILES_DIR%" . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done! Standalone JAR file generated at %JARFILE%) else echo Problems while building jar-file...

popd
goto end

:error
echo There were errors...
goto end

:end
endlocal
