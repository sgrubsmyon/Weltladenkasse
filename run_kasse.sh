#!/bin/bash

runprofiler=false
if [ "$1" == "-p" ]; then
    runprofiler=true
fi

build_dir=build
lib_dir=../lib
main_class=org.weltladen_bonn.pos.kasse.Kasse

cp config.properties $build_dir
cp -r vorlagen $build_dir
cd $build_dir
if [ $runprofiler == true ]; then
    java -javaagent:$HOME/bin/profiler4j-1.0-beta2/agent.jar -cp "$lib_dir/*":. $main_class
else
    java -cp "$lib_dir/*":. $main_class
fi
rm config.properties
rm -r vorlagen
