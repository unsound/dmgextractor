#!/bin/sh
java -cp dist/lib/dmgextractor.jar:dist/lib/apache-ant-1.7.0-bzip2.jar:dist/lib/filedrop.jar:dist/lib/iharder-base64.jar:dist/lib/swing-layout-1.0.1-stripped.jar org.catacombae.dmgx.DMGInfoWindow $1 $2 $3 $4 $5 $6 $7 $8 $9
