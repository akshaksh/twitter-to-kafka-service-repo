#!/bin/bash
# check-keycloak-server-started.sh

curlResult=$(curl -s -o /dev/null -I -w "%{http_code}" http://keycloak-authorization-server:9091/auth/realms/microservices-realm)

echo "result status code for keycloak-server:" "$curlResult"

while [[ ! $curlResult == "200" ]]; do
  >&2 echo "Keycloak server is not up yet!"
  sleep 2
  curlResult=$(curl -s -o /dev/null -I -w "%{http_code}" http://keycloak-authorization-server:9091/auth/realms/microservices-realm)
  echo "result status code inside the loop check for keycloak-server:" "$curlResult"
done

/cnb/process/web
