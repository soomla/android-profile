#!/bin/sh

if [ ! -d libs ];
then
    mkdir libs
fi

if [ ! -f libs/twitter4j-async-4.0.2.jar ];
then
    curl -o libs/twitter4j-async-4.0.2.jar http://twitter4j.org/maven2/org/twitter4j/twitter4j-async/4.0.2/twitter4j-async-4.0.2.jar
fi

if [ ! -f libs/twitter4j-core-4.0.2.jar ];
then
    curl -o libs/twitter4j-core-4.0.2.jar http://twitter4j.org/maven2/org/twitter4j/twitter4j-core/4.0.2/twitter4j-core-4.0.2.jar
fi