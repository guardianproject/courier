#!/bin/bash

# Make sure output dir exists
#
OUTPUTDIR=$1
OUTPUTDIR=$(echo $OUTPUTDIR | sed "s/\/\$//g")
if [ ! -d "$OUTPUTDIR" ]; then
    echo "Error: output directory not found! It needs to be the src/<flavor>/res folder."
    exit
fi

shift

if [ "$1" = "--all" ]; then
    files=*.svg
else
    files="$@"
fi

for f in $files
do
	echo "Processing: $f"

	fout=${f/.svg}
	fout=${fout##*/}
	lang=""
	
	if [[ $fout =~ .*"_farsi" ]]; then
		fout=${fout/"_farsi"}
		lang="-ar"
	fi

	mkdir -p "$OUTPUTDIR/drawable${lang}-xhdpi"
	mkdir -p "$OUTPUTDIR/drawable${lang}-hdpi"
	mkdir -p "$OUTPUTDIR/drawable${lang}-mdpi"
	mkdir -p "$OUTPUTDIR/drawable${lang}-ldpi"

	convert -strip -background none $f ${OUTPUTDIR}/drawable${lang}-xhdpi/${fout}.png
	convert -strip -background none $f -resize 75% ${OUTPUTDIR}/drawable${lang}-hdpi/${fout}.png
	convert -strip -background none $f -resize 50% ${OUTPUTDIR}/drawable${lang}-mdpi/${fout}.png
	convert -strip -background none $f -resize 37.5% ${OUTPUTDIR}/drawable${lang}-ldpi/${fout}.png
done
