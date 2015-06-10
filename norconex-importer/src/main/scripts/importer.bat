@echo off
cd %~dp0

REM Third-party libraries sometimes have to create and write to temporary files.
REM By default those are created in your system "temp" folder 
REM (usually defined under %TEMP% variable in Windows).
REM To change the temporary location those libraries will use, add the
REM following to the java command below (replacing the path):
REM
REM     -Djava.io.tmpdir="C:\temp"

java -Dlog4j.configuration="file:///%ROOT_DIR%classes/log4j.properties" -Dfile.encoding=UTF8 -cp "./lib/*;./classes" com.norconex.importer.Importer %*
