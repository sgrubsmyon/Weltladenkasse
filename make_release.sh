#!/bin/bash

version=v0.9.1

releasedir=../../releases/$version
if [ ! -e $releasedir ]; then
    mkdir $releasedir
fi
rsync -aPvci Weltladenkasse_$version.jar $releasedir
rsync -aPvci Weltladenbesteller_$version.jar $releasedir
rsync -aPvci Bestellvorlage.ods $releasedir
rsync -aPvci Artikelliste.ods $releasedir
rsync -aPvci README.txt $releasedir
rsync -aPvci install-ubuntu.sh $releasedir
rsync -aPvci --exclude=".*" mysql $releasedir
