#!/bin/bash

version=v2.0.1

releasedir=../releases/Weltladenkasse_${version}
trainingdir=../releases/Weltladenkasse_${version}_training
if [ ! -e $releasedir ]; then
    mkdir -p $releasedir
fi
if [ ! -e $trainingdir ]; then
    mkdir -p $trainingdir
fi
rsync -aPvci Weltladenkasse_$version.jar $releasedir
rsync -aPvci Weltladenbesteller_$version.jar $releasedir
rsync -aPvci Weltladenkasse.bat $releasedir
rsync -aPvci Weltladenbesteller.bat $releasedir
rsync -aPvci config.properties $releasedir
rsync -aPvci config_Windows.properties $releasedir
rsync -aPvci config_log4j2.xml $releasedir
rsync -aPvci config_tse.txt $releasedir
#rsync -aPvci dll/ $releasedir
rsync -aPvci --delete --exclude=".*" vorlagen $releasedir
rsync -aPvci README.md $releasedir
rsync -aPvci install-ubuntu.sh $releasedir
rsync -aPvci install-arch.sh $releasedir
rsync -aPvci --delete --exclude=".*" mysql $releasedir

rsync -aPvci Weltladenkasse_$version.jar $trainingdir
rsync -aPvci Weltladenbesteller_$version.jar $trainingdir
rsync -aPvci Weltladenkasse.bat $trainingdir
rsync -aPvci Weltladenbesteller.bat $trainingdir
rsync -aPvci config_training.properties $trainingdir/config.properties
rsync -aPvci config_Windows.properties $trainingdir
rsync -aPvci config_log4j2.xml $trainingdir
rsync -aPvci --delete --exclude=".*" vorlagen $trainingdir
rsync -aPvci README.md $trainingdir
rsync -aPvci install-ubuntu.sh $trainingdir
rsync -aPvci install-arch.sh $trainingdir
rsync -aPvci --delete --exclude=".*" mysql $trainingdir

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
