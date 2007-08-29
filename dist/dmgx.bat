@echo off
setlocal
set DMGX_CP="%~dp0lib\dmgextractor.jar";"%~dp0lib\apache-ant-1.7.0-bzip2.jar";"%~dp0lib\iharder-base64.jar"
java -cp %DMGX_CP% org.catacombae.dmgx.DMGExtractor -startupcommand dmgx %1 %2 %3 %4 %5 %6 %7 %8 %9
endlocal
