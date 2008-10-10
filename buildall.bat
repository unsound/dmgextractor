@echo off
:begin
setlocal

set SOURCES_DIR=%~dp0src
set BUILDTOOLS_CP=%~dp0lib\buildenumerator.jar

pushd %~dp0

echo Incrementing build number...
java -cp "%BUILDTOOLS_CP%" BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgextractor\BuildNumber.java" 1
if not "%ERRORLEVEL%"=="0" goto error

echo Building with ant...
call ant build-application
if "%ERRORLEVEL%"=="0" (echo Done!) else echo Problems while building with ant... && goto error

popd
goto end

:error
echo There were errors...
echo Decrementing build number...
java -cp "%BUILDTOOLS_CP%" BuildEnumerator "%SOURCES_DIR%\org\catacombae\dmgextractor\BuildNumber.java" -1
goto end

:end
endlocal
