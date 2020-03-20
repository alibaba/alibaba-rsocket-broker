# start reactive service responder
start_responder:
  java -jar rsocket-responder/target/rsocket-responder-1.0.0-SNAPSHOT.jar

# start reactive service requester
start_requester:
  java -jar rsocket-requester/target/rsocket-requester-1.0.0-SNAPSHOT.jar

# requester http testing
requester_testing:
  curl http://localhost:8181/user/2

#bench test: 30 seconds, using 5 threads, and keeping 50 HTTP connections open
benchmarking_rpc:
  wrk -t5 -c50 -d30s --latency http://localhost:8181/user/2

benchmarking_fnf:
  wrk -t5 -c50 -d30s --latency http://localhost:8181/job1

benchmarking_stream:
  wrk -t5 -c50 -d30s --latency http://localhost:8181/users

benchmarking_channel:
  wrk -t5 -c50 -d30s --latency http://localhost:8181/channel2
