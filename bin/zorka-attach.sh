#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

RUNDIR=`pwd`
TOOLS_PATH="$JAVA_HOME/lib/tools.jar"
export PATH="$PATH:$JAVA_HOME/jre/bin/"
LIBPATH="$SCRIPTPATH/../*:$SCRIPTPATH/../lib/*:$TOOLS_PATH"
java -classpath "$LIBPATH" com.jkoolcloud.tnt4j.streams.utils.ZorkaAttach $1 $2
tnt4j-streams.sh $3