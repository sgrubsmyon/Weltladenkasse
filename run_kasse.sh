#!/bin/bash

runprofiler=false
if [ "$1" == "-p" ]; then
    runprofiler=true
fi

build_dir=build
lib_dir=../lib
main_class=org.weltladen_bonn.pos.kasse.Kasse
jvm_options="-Djavax.xml.accessExternalDTD=all"

cp config_local.properties $build_dir/config.properties
cp config_log4j2.xml $build_dir
cp config_tse.txt $build_dir
cp -r dsfinv-k $build_dir
cp -r vorlagen $build_dir
#cp -r dll $build_dir
cd $build_dir
java=java
if [ "$JAVA_HOME" != "" ]; then
    java="$JAVA_HOME/bin/java"
fi
if [ $runprofiler == true ]; then
    $java -javaagent:$HOME/bin/profiler4j-1.0-beta2/agent.jar -cp "$lib_dir/*":. "$jvm_options" $main_class
else
    $java -cp "$lib_dir/*":. "$jvm_options" $main_class
fi
rm config.properties
rm config_log4j2.xml
rm config_tse.txt
rm -r dsfinv-k
rm -r vorlagen
#rm -r dll
