#!/bin/sh

SOURCEFILES="../src/org/catacombae/dmgx/*.java ../src/org/catacombae/dmgx/gui/*.java ../src/org/catacombae/io/*.java ../src/org/catacombae/xml/*.java ../src/org/catacombae/xml/apx/*.java"
CLASSPATH=../lib/filedrop.jar:../lib/swing-layout-1.0.1-stripped.jar


rm -r ./doc.~
mkdir ./doc.~
cd doc.~
javadoc -private -classpath $CLASSPATH -link http://java.sun.com/j2se/1.5.0/docs/api/ $SOURCEFILES
cd ..
echo Javadoc constructed in directory doc.~.
