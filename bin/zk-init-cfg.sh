#! /bin/bash
RUNDIR=`pwd`
MAINCLASS="com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigInit"
tnt4j-streams -c -f:$RUNDIR/../config/zk-init-cfg.properties