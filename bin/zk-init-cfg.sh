#! /bin/bash
SCRIPTPATH=`realpath $0`
RUNDIR=`dirname $SCRIPTPATH`
MAINCLASS="com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigInit"
tnt4j-streams.sh -c -f:$RUNDIR/../config/zk-init-cfg.properties