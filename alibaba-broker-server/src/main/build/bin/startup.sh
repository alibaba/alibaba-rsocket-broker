#!/bin/bash
PRGDIR=$(dirname "$0")
JAR_FILE=alibaba-rsocket-broker.jar

if [ ! -e "$PRGDIR/logs" ]; then
  mkdir -p "$PRGDIR/logs"
fi

java "${JAVA_OPTS}" -jar "${PRGDIR}"/lib/${JAR_FILE} &
echo $! >"${PRGDIR}"/app.pid
echo "Begin to start Alibaba RSocket Broker."