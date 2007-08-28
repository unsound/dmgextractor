@echo off
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

echo Extracting swing-layout to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "..\%LIBRARY_PATH%\swing-layout-1.0.1-stripped.jar"
popd

echo Extracting filedrop to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "..\%LIBRARY_PATH%\filedrop.jar"
popd

echo Extracting Apache bzip2 libraries to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "..\%LIBRARY_PATH%\apache-ant-1.7.0-bzip2.jar"
popd

echo Incrementing build number...
java -cp %BUILDTOOLS_CP% BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgx\BuildNumber.java" 1

echo Compiling org.catacombae.dmgx...
javac -sourcepath %SOURCES_DIR% -classpath %BUILD_CP% -d %CLASSFILES_DIR% -Xlint:unchecked %SOURCES_DIR%\org\catacombae\dmgx\*.java
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.dmgx.gui...
javac -sourcepath %SOURCES_DIR% -classpath %BUILD_CP% -d %CLASSFILES_DIR% -Xlint:unchecked %SOURCES_DIR%\org\catacombae\dmgx\gui\*.java
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
echo Decrementing build number...
java -cp %BUILDTOOLS_CP% BuildEnumerator %SOURCES_DIR%\org\catacombae\dmgx\BuildNumber.java -1
goto end

:end
endlocal
