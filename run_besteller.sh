#!/bin/bash

build_dir=build/besteller
lib_dir=../../lib
main_class=Weltladenbesteller.Besteller

cp config.properties $build_dir
cp -r vorlagen $build_dir
cd $build_dir
java -cp "$lib_dir/*":. $main_class
rm config.properties
rm -r vorlagen
