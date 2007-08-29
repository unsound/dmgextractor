@echo off
:begin
setlocal

set SOURCES_DIR=%~dp0src
set CLASSFILES_DIR=build.~
set BUILD_LIB_PATH=%~dp0lib
set DIST_LIB_PATH=%~dp0dist\lib
set JARFILE=dmgextractor.jar
set MANIFEST=%~dp0meta\manifest.txt
set BUILD_CP=%CLASSFILES_DIR%
set BUILDTOOLS_CP=%BUILD_LIB_PATH%\buildenumerator.jar

pushd %~dp0

echo Removing all class files...
if exist %CLASSFILES_DIR% rmdir /s /q %CLASSFILES_DIR%
mkdir %CLASSFILES_DIR%

echo Extracting swing-layout to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\swing-layout-1.0.1-stripped.jar"
popd

echo Extracting filedrop to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\filedrop.jar"
popd

echo Extracting Apache bzip2 libraries to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\apache-ant-1.7.0-bzip2.jar"
popd

echo Extracting iharder-base64 to classfiles directory...
pushd %CLASSFILES_DIR%
jar xf "%DIST_LIB_PATH%\iharder-base64.jar"
popd

echo Incrementing build number...
java -cp "%BUILDTOOLS_CP%" BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgx\BuildNumber.java" 1

echo Compiling org.catacombae.xml...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\xml\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.xml.apx...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\xml\apx\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.io...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\io\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.dmgx...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\dmgx\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.dmgx.gui...
javac -sourcepath "%SOURCES_DIR%" -classpath "%BUILD_CP%" -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\dmgx\gui\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file...
if not exist "%DIST_LIB_PATH%" mkdir "%DIST_LIB_PATH%"
jar cvfm "%DIST_LIB_PATH%\%JARFILE%" "%MANIFEST%" -C "%CLASSFILES_DIR%" . >NUL:
if "%ERRORLEVEL%"=="0" (echo Done!) else echo Problems while building jar-file...

popd
goto end

:error
echo There were errors...
echo Decrementing build number...
java -cp "%BUILDTOOLS_CP%" BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgx\BuildNumber.java" -1
goto end

:end
endlocal
