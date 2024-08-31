#!/bin/sh

mkdir -p bin

cd src

export CLASSPATH=$CLASSPATH:"../libs/*"

echo "Compiling"
scalac *scala -d ../bin/

cd ..
echo "Classes are in ./bin/"
