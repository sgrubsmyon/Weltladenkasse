#!/bin/bash

build_dir=build/kasse
lib_dir=../../lib
classpath=$lib_dir/mysql-connector-java-5.1.25-bin.jar:$lib_dir/jOpenDocument-1.3.jar:$lib_dir/date4j.jar:.
main_class=Weltladenkasse.Kasse

cp config.properties $build_dir
cp -r vorlagen $build_dir
cd $build_dir
java -cp $classpath $main_class
rm config.properties
rm -r vorlagen
