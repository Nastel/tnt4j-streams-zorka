@echo OFF
set RUNDIR=%~p0
set PATH=%PATH%;"%JAVA_HOME%\jre\bin\"
set CLASSPATH=%RUNDIR%..\lib\*";"%RUNDIR%..\lib_dep\*;"%JAVA_HOME%\lib\tools.jar"
java -classpath %CLASSPATH% com.jkoolcloud.tnt4j.streams.utils.ZorkaAttach %1 %2 
call %RUNDIR%\tnt4j-streams.bat %3

