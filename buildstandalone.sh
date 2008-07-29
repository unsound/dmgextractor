#!/bin/sh

SOURCES_DIR=src
BUILD_DIR=build.~
DIST_LIB_PATH=dist/lib
BUILD_LIB_PATH=lib
MANIFEST=doc/standalone-manifest/manifest.txt
BUILD_CP=$BUILD_DIR:dist/lib/catacombae_io.jar:dist/lib/apache-ant-1.7.0-bzip2.jar:dist/lib/filedrop.jar:dist/lib/iharder-base64.jar:dist/lib/swing-layout-1.0.1-stripped.jar
#BUILDTOOLS_CP=$BUILD_LIB_PATH/buildenumerator.jar
JARFILE_DIR=$BUILD_LIB_PATH
JARFILE=dmgextractor-standalone.jar
COMPILE_OPTIONS=-target 1.5 -source 1.5

error() {
    echo "There were errors..."
    decrement_buildnumber
}
jobCompleted() {
    echo "Done! Standalone JAR file generated at $JARFILE_DIR/$JARFILE"
}

removeclassfiles() {
    if [ -d "$BUILD_DIR" ]; then # if exists $BUILD_DIR...
	echo "Removing all class files..."
	rm -r $BUILD_DIR
    fi
    mkdir $BUILD_DIR
    return $?
}

extractdependencies() {
    echo "Extracting Apache bzip2 libraries to classfiles directory..."
    cd $BUILD_DIR
    jar xf "../$DIST_LIB_PATH/apache-ant-1.7.0-bzip2.jar"
    cd ..
    
    echo "Extracting iharder-base64 to classfiles directory..."
    cd $BUILD_DIR
    jar xf "../$DIST_LIB_PATH/iharder-base64.jar"
    cd ..
}
build() {
    echo "Building org.catacombae.dmgextractor.DMGExtractorGraphical and dependencies..."
    javac $COMPILE_OPTIONS -cp $BUILD_CP -sourcepath $SOURCES_DIR -d $BUILD_DIR -Xlint:unchecked $SOURCES_DIR/org/catacombae/dmgextractor/DMGExtractorGraphical.java
    return $?
}
buildjar() {
    echo "Building jar-file..."
    if [ ! -d "$JARFILE_DIR" ]; then # if not exists $JARFILE_DIR...
	echo "Making library path"
    	mkdir $JARFILE_DIR
    fi
    jar cfm $JARFILE_DIR/$JARFILE $MANIFEST -C $BUILD_DIR .
    return $?
}

main() {
    removeclassfiles
    extractdependencies
    build
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
}

# Entry point
main
