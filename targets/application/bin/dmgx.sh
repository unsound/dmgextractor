#!/bin/sh
DMGX_CP=../lib/dmgextractor.jar
java -cp $DMGX_CP org.catacombae.dmgextractor.DMGExtractor -startupcommand "$0" "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
