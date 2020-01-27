#!/bin/bash
PRGDIR=$(dirname "$0")
JAR_FILE=alibaba-rsocket-broker.jar
mkdir -p logs
nohup java ${JAVA_OPTS} -jar "${PRGDIR}"/lib/${JAR_FILE} > "${PRGDIR}"/logs/application.log 2>&1 &
echo $! > "${PRGDIR}"/app.pid
echo "RSocket Server started."