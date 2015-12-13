#!/bin/sh

if [ ! -d libs ];
then
    mkdir libs
fi

if [ ! -f libs/simple-fb-4.0.9.jar ];
then
    curl -o simple-fb-4.0.9.aar https://dl.bintray.com/sromku/maven/com/sromku/simple-fb/4.0.9/simple-fb-4.0.9.aar
    mkdir simple-fb
    tar -xvf simple-fb-4.0.9.aar -C simple-fb
    mv simple-fb/classes.jar libs/simple-fb-4.0.9.jar
    rm -rf simple-fb
    rm simple-fb-4.0.9.aar
fi

if [ ! -f libs/gson-1.7.2.jar ];
then
    curl -o libs/gson-1.7.2.jar http://central.maven.org/maven2/com/google/code/gson/gson/1.7.2/gson-1.7.2.jar
fi

if [ ! -d libs/facebook ];
then
    git clone https://github.com/facebook/facebook-android-sdk.git libs/facebook --recursive
fi