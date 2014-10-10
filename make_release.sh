#!/bin/bash

version=v0.9.2

releasedir=../../releases/$version
if [ ! -e $releasedir ]; then
    mkdir $releasedir
fi
rsync -aPvci Weltladenkasse_$version.jar $releasedir
rsync -aPvci Weltladenbesteller_$version.jar $releasedir
rsync -aPvci Weltladenkasse.bat $releasedir
rsync -aPvci Weltladenbesteller.bat $releasedir
rsync -aPvci config.properties $releasedir
rsync -aPvci config_Windows.properties $releasedir
rsync -aPvci Bestellvorlage.ods $releasedir
rsync -aPvci Artikelliste.ods $releasedir
rsync -aPvci README.txt $releasedir
rsync -aPvci README_Windows.txt $releasedir
rsync -aPvci install-ubuntu.sh $releasedir
rsync -aPvci --exclude=".*" mysql $releasedir
