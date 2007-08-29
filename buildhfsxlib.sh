#!/bin/sh

error() {
    echo "There were errors..."
}

SOURCES_DIR=src
CLASSFILES_DIR=build.~
BUILD_LIB_PATH=lib
DIST_LIB_PATH=dist/lib
BUILD_CP=$DIST_LIB_PATH/apache-ant-1.7.0-bzip2.jar:$DIST_LIB_PATH/iharder-base64.jar
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
    if [ ! -d "$BUILD_LIB_PATH" ]; then # if not exists $BUILD_LIB_PATH...
	echo "Making library path"
    	mkdir $BUILD_LIB_PATH
    fi
    jar cvf $BUILD_LIB_PATH/$JARFILE -C $CLASSFILES_DIR .
    if [ "$?" == 0 ]; then
	echo "Done! Remember to include dependencies $BUILD_CP in target."
    else
	error
    fi
fi
