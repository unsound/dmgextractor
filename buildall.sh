#!/bin/sh

error() {
    echo "There were errors..."
    echo "Decrementing build number..."
    java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgx/BuildNumber.java -1
}

SOURCES_DIR=src
CLASSFILES_DIR=build.~
LIBRARY_PATH=lib
MANIFEST=meta/manifest.txt
BUILD_CP=$CLASSFILES_DIR
#:$LIBRARY_PATH/filedrop.jar
BUILDTOOLS_CP=buildenumerator/buildenumerator.jar
JARFILE=dmgextractor.jar
#PACKAGES=org.catacombae.dmgx org.catacombae.dmgx.gui
#COMPILEPATHS=org/catacombae/dmgx/*.java org/catacombae/dmgx/gui/*.java

if [ -d "$CLASSFILES_DIR" ]; then # if exists $CLASSFILES_DIR...
    echo "Removing all class files..."
    rm -r $CLASSFILES_DIR
fi
mkdir $CLASSFILES_DIR

echo "Extracting swing-layout to classfiles directory..."
cd $CLASSFILES_DIR
jar xf "../$LIBRARY_PATH/swing-layout-1.0.1-stripped.jar"
cd ..

echo "Extracting filedrop to classfiles directory..."
cd $CLASSFILES_DIR
jar xf "../$LIBRARY_PATH/filedrop.jar"
cd ..

#echo "Extracting base64 to classfiles directory..."
#cd $CLASSFILES_DIR
#jar xf "../$LIBRARY_PATH/base64.jar"
#cd ..

echo "Incrementing build number..."
java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgx/BuildNumber.java 1
echo "Compiling org.catacombae.xml.parser..."
javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $CLASSFILES_DIR -Xlint:deprecation $SOURCES_DIR/org/catacombae/xml/parser/*.java
JAVAC_EXIT_CODE=$?
if [ "$JAVAC_EXIT_CODE" != 0 ]; then
    error
else
    echo "Compiling org.catacombae.dmgx..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $CLASSFILES_DIR -Xlint:deprecation -Xlint:unchecked $SOURCES_DIR/org/catacombae/dmgx/*.java
    JAVAC_EXIT_CODE=$?
    if [ "$JAVAC_EXIT_CODE" == 0 ]; then
        echo "Building jar-file..."
        if [ ! -d "$LIBRARY_PATH" ]; then # if not exists $LIBRARY_PATH...
	    echo "Making library path"
    	    mkdir $LIBRARY_PATH
	fi
	jar cfm $LIBRARY_PATH/$JARFILE $MANIFEST -C $CLASSFILES_DIR .
	if [ "$?" == 0 ]; then
	    echo Done!
	else
	    error
	fi
    else
	error
    fi
fi
