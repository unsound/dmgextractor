#!/bin/sh

SOURCES_DIR=src/java
BUILDTOOLS_CP=lib/buildenumerator.jar

error() {
    echo "There were errors..."
    decrement_buildnumber
    exit 1
}
jobCompleted() {
    echo "Done!"
}

increment_buildnumber() {
    echo "Incrementing build number..."
    java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgextractor/BuildNumber.java 1
}

decrement_buildnumber() {
    echo "Decrementing build number..."
    java -cp $BUILDTOOLS_CP BuildEnumerator $SOURCES_DIR/org/catacombae/dmgextractor/BuildNumber.java -1
}

ant_build() {
    ant build-application
    return $?
}

main() {
    increment_buildnumber
    if [ "$?" -eq 0 ]; then
	ant_build
	if [ "$?" -eq 0 ]; then
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
