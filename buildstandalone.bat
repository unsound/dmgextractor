@echo off
:begin
setlocal

set SOURCES_DIR=%~dp0src

pushd %~dp0

echo Building with ant...
call ant build-standalone
if "%ERRORLEVEL%"=="0" (echo Done!) else echo Problems while building with ant... && goto error

popd
goto end

:error
echo There were errors...
goto end

:end
endlocal
