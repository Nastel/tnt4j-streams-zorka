#! /bin/sh
RUNDIR=`pwd`
CLASSPATH="$RUNDIR/../lib/*:$RUNDIR/../lib_dep/*"
TNT4JOPTS=-Dtnt4j.config="$RUNDIR/../config/tnt4j.properties"
LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%/../config/log4j.properties"
#LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%/../config/logback.xml"
java $LOG4JOPTS $TNT4JOPTS -classpath $CLASSPATH com.jkool.tnt4j.streams.StreamsAgent $*