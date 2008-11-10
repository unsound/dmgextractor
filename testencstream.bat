@echo off

call "%~dp0definevars.bat"

java -cp "%DMGX_CLASSPATH%" org.catacombae.dmg.encrypted.ReadableCEncryptedEncodingStream %1 %2 %3 %4 %5 %6 %7 %8 %9
