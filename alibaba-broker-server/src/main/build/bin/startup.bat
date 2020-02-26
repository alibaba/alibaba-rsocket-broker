@echo off

@REM The script path to reference the included JRE java file
SET SCRIPT_PATH=%~dp0

SET JAR_FILE="%SCRIPT_PATH%\lib\*"

java -classpath %JAR_FILE% com.alibaba.rsocket.broker.AlibabaRSocketBrokerServer