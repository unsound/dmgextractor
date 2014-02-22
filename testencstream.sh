#!/bin/sh

DMGX_CLASSPATH=`./definevars.sh`

java -cp "$DMGX_CLASSPATH" org.catacombae.dmg.encrypted.ReadableCEncryptedEncodingStream "$@"
