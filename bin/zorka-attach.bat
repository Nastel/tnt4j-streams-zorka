@echo off
setlocal

set RUNDIR=%~p0
set PATH=%PATH%;"%JAVA_HOME%\jre\bin\"
set TOOLS_PATH="%JAVA_HOME%\lib\tools.jar"
set CLASSPATH=%CLASSPATH%";"%RUNDIR%..\*";"%RUNDIR%..\lib\*";"%TOOLS_PATH%

@echo on
java -classpath "%CLASSPATH%" com.jkoolcloud.tnt4j.streams.utils.ZorkaAttach %1 %2
call %RUNDIR%\tnt4j-streams.bat %3
