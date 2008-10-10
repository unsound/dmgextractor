#!/bin/sh

SOURCES_DIR=src

error() {
    echo "There were errors..."
    exit 1
}
jobCompleted() {
    echo "Done!"
}

ant_build() {
    ant build-hfsxlib
    return $?
}

main() {
    ant_build
    if [ "$?" == 0 ]; then
	jobCompleted
    else
	error
    fi
}

# Entry point
main
