#!/bin/sh

error() {
    echo "There were errors..."
}

SOURCES_DIR=src
CLASSFILES_DIR=build.~
LIBRARY_PATH=lib
BUILD_CP=$CLASSFILES_DIR
JARFILE=hfsx_dmglib.jar

if [ -d "$CLASSFILES_DIR" ]; then # if exists $CLASSFILES_DIR...
    echo "Removing all class files..."
    rm -r $CLASSFILES_DIR
fi
mkdir $CLASSFILES_DIR

echo "Compiling org.catacombae.dmgx.DmgRandomAccessStream (and dependencies)..."
javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $CLASSFILES_DIR -Xlint:deprecation $SOURCES_DIR/org/catacombae/dmgx/DmgRandomAccessStream.java
JAVAC_EXIT_CODE=$?
if [ "$JAVAC_EXIT_CODE" != 0 ]; then
    error
else
    echo "Building jar-file..."
    if [ ! -d "$LIBRARY_PATH" ]; then # if not exists $LIBRARY_PATH...
	echo "Making library path"
    	mkdir $LIBRARY_PATH
    fi
    jar cvf $LIBRARY_PATH/$JARFILE -C $CLASSFILES_DIR .
    if [ "$?" == 0 ]; then
	echo Done!
    else
	error
    fi
fi
