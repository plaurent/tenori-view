#!/bin/sh

cd src
mkdir -p lib
mkdir -p bin
SCALADIR=`which scala`
SCALADIR=`dirname $SCALADIR`
cp $SCALADIR/../lib/*jar lib/.
cp ../libs/*jar lib/.
rm -f *.jar

echo "Compiling"
scalac *scala -d ./bin/

echo "Unpacking all required libraries for standalone distribution"
cd bin
ls ../lib/*jar | sed "s/^/jar xf /" | sh
cd ..


echo "Building executable JAR"
echo "Main-Class: main" > Manifest.txt
jar cfm tenori-view.jar Manifest.txt lib/*jar -C bin .
mv tenori-view.jar ..

echo "Cleaning up"
rm -rf bin
rm -rf lib
rm Manifest.txt

cd ..
echo "Enjoy tenori-view.jar in `pwd`"
