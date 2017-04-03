#! /bin/bash
SCRIPTPATH=`realpath $0`
RUNDIR=`dirname $SCRIPTPATH`
TOOLS_PATH="$JAVA_HOME/lib/tools.jar"
export PATH=$PATH;"$JAVA_HOME/jre/bin/"
CLASSPATH="$RUNDIR/../*:$RUNDIR/../lib/*:$TOOLS_PATH"
java -classpath ${CLASSPATH} com.jkoolcloud.tnt4j.streams.utils.ZorkaAttach $1 $2
tnt4j-streams.sh $3