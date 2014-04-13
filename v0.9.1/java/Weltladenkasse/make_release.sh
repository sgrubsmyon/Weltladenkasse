#!/bin/bash

version=v0.9

releasedir=../../../../releases/$version
if [ ! -e $releasedir ]; then
    mkdir $releasedir
fi
rsync -aPvci Weltladenkasse_$version.jar $releasedir
rsync -aPvci Weltladenbesteller_$version.jar $releasedir
rsync -aPvci README.txt $releasedir
rsync -aPvci install-ubuntu.sh $releasedir
rsync -aPvci --exclude=".*" mysql $releasedir
#rsync -aPvci ../../Artikel.ods $releasedir
#rsync -aPvci ../../Artikel.csv $releasedir
rsync -aPvci /home/uriuri/Documents/private/Weltladen/kasse/Artikelliste.ods $releasedir
rsync -aPvci /home/uriuri/Documents/private/Weltladen/kasse/Artikelliste.csv $releasedir
