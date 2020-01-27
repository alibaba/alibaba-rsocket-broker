#!/bin/bash
PRGDIR=$(dirname "$0")
jar_file=alibaba-rsocket-broker.jar
JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=${PRGDIR}/config/logback-spring.xml"
mkdir -p logs
nohup java ${JAVA_OPTS} -jar "${PRGDIR}"/lib/${jar_file} >> "${PRGDIR}"/logs/application.log 2>&1 &
echo $! > "${PRGDIR}"/app.pid
echo "RSocket Server started."