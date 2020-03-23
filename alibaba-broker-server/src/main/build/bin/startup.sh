#!/bin/bash
PRGDIR=$(dirname "$0")

if [ ! -e "$PRGDIR/logs" ]; then
  mkdir -p "$PRGDIR/logs"
fi

java -jar "${PRGDIR}"/lib/alibaba-rsocket-broker.jar &
echo $! >"${PRGDIR}"/app.pid
echo "Begin to start Alibaba RSocket Broker."