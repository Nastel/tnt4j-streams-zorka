@echo off
set RUNDIR=%~p0
set CLASSPATH=%RUNDIR%..\lib\*";"%RUNDIR%..\lib_dep\*
set TNT4JOPTS=-Dtnt4j.config="%RUNDIR%..\config\tnt4j.properties"
set LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%..\config\log4j.properties"
REM set LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%..\config\logback.xml"
java %LOG4JOPTS% %TNT4JOPTS% -classpath %CLASSPATH% com.jkool.tnt4j.streams.StreamsAgent %*