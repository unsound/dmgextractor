#!/bin/sh

error() {
    echo "There were errors..."
    decrement_buildnumber
}
jobCompleted() {
    echo "Done!"
}


SOURCES_DIR=src
BUILD_DIR=build.~
DIST_LIB_PATH=dist/lib
BUILD_LIB_PATH=lib
#MANIFEST=meta/manifest.txt
BUILD_CP=$BUILD_DIR:dist/lib/apache-ant-1.7.0-bzip2.jar:dist/lib/filedrop.jar:dist/lib/iharder-base64.jar:dist/lib/swing-layout-1.0.1-stripped.jar
#:$LIBRARY_PATH/filedrop.jar
BUILDTOOLS_CP=$BUILD_LIB_PATH/buildenumerator.jar
JARFILE_DIR=$DIST_LIB_PATH
JARFILE=dmgextractor.jar
#PACKAGES=org.catacombae.dmgx org.catacombae.dmgx.gui
#COMPILEPATHS=org/catacombae/dmgx/*.java org/catacombae/dmgx/gui/*.java

removeclassfiles() {
    if [ -d "$BUILD_DIR" ]; then # if exists $BUILD_DIR...
	echo "Removing all class files..."
	rm -r $BUILD_DIR
    fi
    mkdir $BUILD_DIR
    return $?
}

increment_buildnumber() {
    echo "Incrementing build number..."
    java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgx/BuildNumber.java 1
}
decrement_buildnumber() {
    echo "Decrementing build number..."
    java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgx/BuildNumber.java -1
}

build_xml() {
    echo "Building org.catacombae.xml..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/xml/*.java
    return $?
}
build_xml_apx() {
    echo "Building org.catacombae.xml.apx..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/xml/apx/*.java
    return $?
}
build_io() {
    echo "Building org.catacombae.io..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/io/*.java
    return $?
}
build_udif() {
    echo "Building org.catacombae.udif..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/udif/*.java
    return $?
}
build_dmgx() {
    echo "Building org.catacombae.dmgx..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/dmgx/*.java
    return $?
}
build_dmgx_gui() {
    echo "Building org.catacombae.dmgx.gui..."
    javac -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/dmgx/gui/*.java
    return $?
}
buildjar() {
    echo "Building jar-file..."
    if [ ! -d "$JARFILE_DIR" ]; then # if not exists $LIBRARY_PATH...
	echo "Making library path"
    	mkdir $JARFILE_DIR
    fi
    jar cf $JARFILE_DIR/$JARFILE -C $BUILD_DIR .
    return $?
}

main() {
    removeclassfiles
    increment_buildnumber
    if [ "$?" == 0 ]; then
	build_xml
	if [ "$?" == 0 ]; then
	    build_xml_apx
	    if [ "$?" == 0 ]; then
		build_io
		if [ "$?" == 0 ]; then
		    build_udif
		    if [ "$?" == 0 ]; then
			build_dmgx
			if [ "$?" == 0 ]; then
			    build_dmgx_gui
			    if [ "$?" == 0 ]; then
				buildjar
				if [ "$?" == 0 ]; then
				    jobCompleted
				else
				    error
				fi
			    else
				error
			    fi
			else
			    error
			fi
		    else
			error
		    fi
		else
		    error
		fi
	    else
		error
	    fi
	else
	    error
	fi
    else
	error
    fi
}

# Entry point
main
