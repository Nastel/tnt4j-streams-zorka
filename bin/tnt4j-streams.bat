@echo off
set RUNDIR=%~p0
set CLASSPATH=%RUNDIR%..\jars\*";"%RUNDIR%..\libs\*
set TNT4JOPTS=-Dtnt4j.config="%RUNDIR%..\config\tnt4j.properties"
set LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%..\config\log4j.properties"
REM set LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%..\config\logback.xml"
java %LOG4JOPTS% %TNT4JOPTS% -classpath %CLASSPATH% com.jkoolcloud.tnt4j.streams.StreamsAgent %*