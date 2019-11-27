# setup ToxiProxy
setup:
   brew tap shopify/shopify
   brew install toxiproxy

# start ToxiProxy
start_proxy:
   toxiproxy-server -config toxiproxy.json &

# create proxy channel for broker
create_broker_proxy_1:
   toxiproxy-cli create broker_proxy_1 --listen 0.0.0.0:19999 --upstream localhost:9999
