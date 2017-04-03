#! /bin/bash
SCRIPTPATH=`realpath $0`
RUNDIR=`dirname $SCRIPTPATH`
CLASSPATH="$CLASSPATH:$RUNDIR/../*:$RUNDIR/../lib/*"
TNT4JOPTS="-Dtnt4j.config=$RUNDIR/../config/tnt4j.properties"
LOG4JOPTS="-Dlog4j.configuration=file:$RUNDIR/../config/log4j.properties"
#LOGBACKOPTS="-Dlogback.configurationFile=file:$RUNDIR/../config/logback.xml"
STREAMSOPTS="$STREAMSOPTS $LOG4JOPTS $TNT4JOPTS"

if [ "$MAINCLASS" == "" ]; then
	MAINCLASS="com.jkoolcloud.tnt4j.streams.StreamsAgent"
fi

java ${STREAMSOPTS} -classpath ${CLASSPATH} ${MAINCLASS} $*