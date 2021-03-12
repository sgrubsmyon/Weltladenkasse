#!/bin/bash

runprofiler=false
if [ "$1" == "-p" ]; then
    runprofiler=true
fi

build_dir=build
lib_dir=../lib
main_class=com.cryptovision.SEAPI.test.Operational

cp config_local.properties $build_dir/config.properties
cp config_log4j2.xml $build_dir
cp config_tse.txt $build_dir/config.txt
cp -r vorlagen $build_dir
cd $build_dir
java=java
if [ "$JAVA_HOME" != "" ]; then
    java="$JAVA_HOME/bin/java"
fi
if [ $runprofiler == true ]; then
    $java -javaagent:$HOME/bin/profiler4j-1.0-beta2/agent.jar -cp "$lib_dir/*":. $main_class
else
    $java -cp "$lib_dir/*":. $main_class
fi
rm config.properties
rm config_log4j2.xml
rm config.txt
rm -r vorlagen
