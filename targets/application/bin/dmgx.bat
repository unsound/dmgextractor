@echo off
setlocal
set DMGX_CP="%~dp0..\lib\dmgextractor.jar";"%~dp0..\lib\apache-ant-1.7.0-bzip2.jar";"%~dp0..\lib\iharder-base64.jar";"%~dp0..\lib\catacombae_io.jar"
java -cp %DMGX_CP% org.catacombae.dmgextractor.DMGExtractor -startupcommand dmgx %1 %2 %3 %4 %5 %6 %7 %8 %9
endlocal
