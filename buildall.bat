@echo off
:begin
setlocal

set SOURCES_DIR=%~dp0src
set CLASSFILES_DIR=build.~
set BUILD_LIB_PATH=%~dp0lib
set DIST_LIB_PATH=%~dp0dist\lib
set JARFILE=%DIST_LIB_PATH%\dmgextractor.jar
set BUILD_CP="%CLASSFILES_DIR%";"%DIST_LIB_PATH%\apache-ant-1.7.0-bzip2.jar";"%DIST_LIB_PATH%\filedrop.jar";"%DIST_LIB_PATH%\iharder-base64.jar";"%DIST_LIB_PATH%\swing-layout-1.0.1-stripped.jar"
set BUILDTOOLS_CP=%BUILD_LIB_PATH%\buildenumerator.jar

pushd %~dp0

echo Removing all class files...
if exist %CLASSFILES_DIR% rmdir /s /q %CLASSFILES_DIR%
mkdir %CLASSFILES_DIR%

echo Incrementing build number...
java -cp "%BUILDTOOLS_CP%" BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgx\BuildNumber.java" 1

echo Compiling org.catacombae.xml...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\xml\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.xml.apx...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\xml\apx\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.io...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\io\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.udif...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\udif\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.dmgx...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\dmgx\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Compiling org.catacombae.dmgx.gui...
javac -sourcepath "%SOURCES_DIR%" -classpath %BUILD_CP% -d "%CLASSFILES_DIR%" -Xlint:unchecked \\.\\"%SOURCES_DIR%\org\catacombae\dmgx\gui\*.java"
set JAVAC_EXIT_CODE=%ERRORLEVEL%
if not "%JAVAC_EXIT_CODE%"=="0" goto error

echo Building jar-file...
if not exist "%DIST_LIB_PATH%" mkdir "%DIST_LIB_PATH%"
jar cvf "%JARFILE%" -C "%CLASSFILES_DIR%" . >NUL:
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
