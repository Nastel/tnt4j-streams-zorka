#!/bin/sh

echo installing libraries required by Zorka...
mvn install:install-file -Dfile="${BASH_SOURCE%/*}/zico-util.jar" -DgroupId=zorka -DartifactId=zico-util -Dversion=1.0.0 -Dpackaging=jar

echo DONE!

#read -p "Press [Enter] key to exit..."