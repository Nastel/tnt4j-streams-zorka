@echo OFF

setlocal

if "%M2_HOME%" == "" goto try_maven_home
set MVN=%M2_HOME%\bin\mvn
if not exist "%MVN%" goto try_maven_home
goto run_mvn

:try_maven_home
if "%MAVEN_HOME%" == "" goto try_path
set MVN=%MAVEN_HOME%\bin\mvn
if not exist "%MVN%" goto try_path
goto run_mvn

:try_path
set MVN=C:\maven\bin\mvn

:run_mvn

echo installing libraries required by Zorka...
call %MVN% install:install-file -Dfile=zico-util.jar -DgroupId=zorka -DartifactId=zico-util -Dversion=1.0.0 -Dpackaging=jar
call %MVN% install:install-file -Dfile=zorka.jar -DgroupId=zorka -DartifactId=zorka -Dversion=1.0.0 -Dpackaging=jar

echo DONE!

pause