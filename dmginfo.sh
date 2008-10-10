#!/bin/sh

./definevars.sh

java -cp $DMGX_CLASSPATH org.catacombae.dmgx.DMGInfoWindow $1 $2 $3 $4 $5 $6 $7 $8 $9
