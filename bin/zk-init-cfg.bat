@echo off
set RUNDIR=%~p0
set MAINCLASS=com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigInit
tnt4j-streams.bat -c -f:%RUNDIR%\..\config\zk-init-cfg.properties