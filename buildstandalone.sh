#!/bin/sh

error() {
    echo "There were errors..."
    exit 1
}
jobCompleted() {
    echo "Done!"
}

ant_build() {
    ant build-standalone
    return $?
}

main() {
    ant_build
    if [ "$?" -eq 0 ]; then
	jobCompleted
    else
	error
    fi
}

# Entry point
main
