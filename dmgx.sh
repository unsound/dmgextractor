#!/bin/sh
BASE=dist
DMGX_CP=$BASE/lib/dmgextractor.jar:$BASE/lib/apache-ant-1.7.0-bzip2.jar:$BASE/lib/iharder-base64.jar
java -cp $DMGX_CP org.catacombae.dmgx.DMGExtractor -startupcommand "$0" "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
