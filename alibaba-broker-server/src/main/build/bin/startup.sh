#!/bin/bash
PRGDIR=$(dirname "$0")

if [ ! -e "$PRGDIR/logs" ]; then
  mkdir -p "$PRGDIR/logs"
fi

java "${JAVA_OPTS}" -classpath "${PRGDIR}"/lib/* com.alibaba.rsocket.broker.AlibabaRSocketBrokerServer &
echo $! >"${PRGDIR}"/app.pid
echo "Begin to start Alibaba RSocket Broker."