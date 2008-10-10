#!/bin/sh

export LIBDIR="targets/application/lib"

export DMGX_CLASSPATH="$LIBDIR/dmgextractor.jar:$LIBDIR/catacombae_io.jar:$LIBDIR/apache-ant-1.7.0-bzip2.jar:$LIBDIR/filedrop.jar:$LIBDIR/iharder-base64.jar:$LIBDIR/swing-layout-1.0.3.jar"
echo "$DMGX_CLASSPATH"
