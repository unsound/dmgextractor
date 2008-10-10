#!/bin/sh

DMGX_CLASSPATH=`./definevars.sh`

java -cp "$DMGX_CLASSPATH" org.catacombae.dmgextractor.DMGExtractor -startupcommand "$0" "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
