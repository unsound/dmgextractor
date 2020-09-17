#!/bin/bash

match () {
    local SUBFILE="$1"
    local DIRNAME="$2"
    CMPRES=`expr "${SUBFILE}" : "${DIRNAME}"`
    RETVAL="$?"
    if [ "${RETVAL}" -eq 0 ]; then
	if [ -d "${SUBFILE}" ]; then
	    echo "`pwd`/${SUBFILE}/"
	    rm -r "${SUBFILE}"
	    if [ ! $? -eq 0 ]; then return 1; fi
	else
	    echo "`pwd`/${SUBFILE}"
	    rm "${SUBFILE}"
	    if [ ! $? -eq 0 ]; then return 1; fi
	fi
    elif [ "${RETVAL}" -eq 1 ]; then
	if [ -d "${SUBFILE}" ]; then
	    recursiveRmdir "${DIRNAME}" "${SUBFILE}"
	    if [ ! $? -eq 0 ]; then return 1; fi
	fi
    fi
    return 0
}

recursiveRmdir () {
    local DIRNAME="$1"
    #local DIRNAMELEN="${#DIRNAME}"
    shift 1
    while [ ! $# -eq 0 ]; do
	local FILE="$1"
	shift 1
	
	if [ -d "${FILE}" ]; then
	    
	    pushd "${FILE}" > /dev/null
	    
	    for SUBFILE in .*; do
		if [ ! "${SUBFILE}" = ".." ] && [ ! "${SUBFILE}" = "." ]; then
		    match "${SUBFILE}" "${DIRNAME}"
		    if [ ! $? -eq 0 ]; then return 1; fi
		fi
	    done		    
	    
	    for SUBFILE in *; do
		if [ ! "${SUBFILE}" = "*" ]; then
		    match "${SUBFILE}" "${DIRNAME}"
		    if [ ! $? -eq 0 ]; then return 1; fi
		fi
	    done
	    
	    popd > /dev/null
	else
	    match "${FILE}" "${DIRNAME}"
	    if [ ! $? -eq 0 ]; then return 1; fi
	fi
    done
    return 0
}

error () {
    echo "There were errors..."
    exit 1
}

checkerror () {
    if [ ! $1 -eq 0 ]; then error; fi
}

TEMPDIR="srcdisttemp.~"
OUTFILE="releases/current-src.zip"

copydir () {
    cp -r $1 "${TEMPDIR}" 
    checkerror $?
}

echo "Cleaning temp dir..."
rm -r "${TEMPDIR}"
mkdir "${TEMPDIR}"
checkerror $?

echo "Copying files..."
cp *.sh ${TEMPDIR} 
checkerror $?
cp *.bat ${TEMPDIR} 
checkerror $?
cp *.xml ${TEMPDIR} 
checkerror $?

copydir lib
copydir src
copydir targets

echo "Removing CVS directories..."
recursiveRmdir "^CVS$" "${TEMPDIR}"
checkerror $?
echo "Removing emacs backup files (*~)..."
recursiveRmdir ".*~$" "${TEMPDIR}"
checkerror $?
echo "Removing emacs temporary files (#*#)..."
recursiveRmdir "^#.*#$" "${TEMPDIR}"
checkerror $?
echo "Removing Thumbs.db files..."
recursiveRmdir "^Thumbs\.db$" "${TEMPDIR}"
checkerror $?
echo "Removing .DS_Store files..."
recursiveRmdir "^\.DS_Store$" "${TEMPDIR}"
checkerror $?
echo "Removing .cvsignore files..."
recursiveRmdir "^\.cvsignore$" "${TEMPDIR}"
checkerror $?

echo "Setting execute permissions for shell scripts..."
chmod a+x ${TEMPDIR}/*.sh
checkerror $?
chmod a+x ${TEMPDIR}/targets/application/bin/*.sh
checkerror $?

echo "Building zip file..."
cd "${TEMPDIR}"
rm "../${OUTFILE}"
zip -9 -r "../${OUTFILE}" *
checkerror $?
cd ..

echo "Done! Zip file generated in ${OUTFILE}"
