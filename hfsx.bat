@echo off
pushd %~dp0
java -cp lib\dmgexplorer.jar HFSExplorer %1 %2 %3 %4 %5 %6 %7 %8 %9
popd
