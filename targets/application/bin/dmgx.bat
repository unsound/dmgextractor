@echo off
setlocal
set DMGX_CP="%~dp0..\lib\dmgextractor.jar"
java -cp %DMGX_CP% org.catacombae.dmgextractor.DMGExtractor -startupcommand dmgx %1 %2 %3 %4 %5 %6 %7 %8 %9
endlocal
