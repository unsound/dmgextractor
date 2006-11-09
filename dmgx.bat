@echo off
pushd %~dp0
java -cp lib\dmgextractor.jar org.catacombae.dmgx.DMGExtractor -startupcommand dmgx %1 %2 %3 %4 %5 %6 %7 %8 %9
popd
