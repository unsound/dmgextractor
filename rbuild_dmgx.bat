@echo off
REM ************************************************************************************************
REM * This script is for building a release of DMGExtractor. It should not be used in development. *
REM * It does not increment the build number, and only compiles the classes that are dependencies  *
REM * of org.catacombae.dmgx.DMGExtractorGraphical, as it is the main class of the executable jar. *
REM ************************************************************************************************

:begin
setlocal

set SOURCES_DIR=src
set CLASSFILES_DIR=build.~
set LIBRARY_PATH=lib
set JARFILE=dmgextractor.jar
set MANIFEST=meta\manifest.txt
set BUILD_CP=%CLASSFILES_DIR%
set BUILDTOOLS_CP=buildenumerator\buildenumerator.jar

pushd %~dp0

echo Removing all class files...
if exist %CLASSFILES_DIR% rmdir /s /q %CLASSFILES_DIR%
mkdir %CLASSFILES_DIR%

echo Compiling org.catacombae.dmgx.DMGExtractorGraphical (and dependencies)...
javac -sourcepath %SOURCES_DIR% -classpath %BUILD_CP% -d %CLASSFILES_DIR% -Xlint:unchecked %SOURCES_DIR%\org\catacombae\dmgx\DMGExtractorGraphical.java
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file %LIBRARY_PATH%\%JARFILE%...
if not exist %LIBRARY_PATH% mkdir %LIBRARY_PATH%
jar cvfm %LIBRARY_PATH%\%JARFILE% %MANIFEST% -C %CLASSFILES_DIR% . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done!) else echo Problems while building jar-file...

popd
goto end

:error
echo There were errors...

:end
endlocal
