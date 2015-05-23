#!/bin/bash

version=v0.9.7

releasedir=../releases/Weltladenkasse_$version
if [ ! -e $releasedir ]; then
    mkdir -p $releasedir
fi
rsync -aPvci Weltladenkasse_$version.jar $releasedir
rsync -aPvci Weltladenbesteller_$version.jar $releasedir
rsync -aPvci Weltladenkasse.bat $releasedir
rsync -aPvci Weltladenbesteller.bat $releasedir
rsync -aPvci config.properties $releasedir
rsync -aPvci config_Windows.properties $releasedir
rsync -aPvci --delete --exclude=".*" vorlagen $releasedir
rsync -aPvci README.md $releasedir
rsync -aPvci install-ubuntu.sh $releasedir
rsync -aPvci --delete --exclude=".*" mysql $releasedir

cd $releasedir
if [ -e ../Weltladenkasse_$version.zip ]; then
    rm ../Weltladenkasse_$version.zip
fi
zip -r ../Weltladenkasse_$version.zip *
cd ..
if [ -e Weltladenkasse_$version.tar.gz ]; then
    rm Weltladenkasse_$version.tar.gz
fi
tar -czvf Weltladenkasse_$version.tar.gz Weltladenkasse_$version

exit 0
