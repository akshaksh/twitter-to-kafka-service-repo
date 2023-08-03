#!/bin/bash
# check-config-server-started.sh

apt-get update -y
yes | apt-get install curl

#run th curl command against config server actuator health end point and extract the http code
curlResult=$(curl -s -o /dev/null -I -w "%{http_code}" http://config-server:8888/actuator/health)

#print the result of http status code
echo "result status code for config-server:" "$curlResult"

#Run a simple loop and check if we got correct response code from the actuator endpoint of config-server
#and if application is not up and running we will print the message on console and wait for 2 seconds.
while [[ ! $curlResult == "200" ]]; do
  >&2 echo "Config server is not up yet!"
  sleep 2
  curlResult=$(curl -s -o /dev/null -I -w "%{http_code}" http://config-server:8888/actuator/health)
  echo "result status code inside the loop check for config-server:" "$curlResult"
done

check-keycloak-server-started.sh

