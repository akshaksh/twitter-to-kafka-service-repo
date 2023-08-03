version: '3.7'
services:
  keycloak-authorization-server:
    image: quay.io/keycloak/keycloak:${KEYCLOAK_VERSION:-latest}
    hostname: keycloak-server
    entrypoint: [ "/opt/keycloak/bin/kc.sh", "start-dev", "--http-relative-path=/auth", "--http-port=9091" ]
    ports:
      - "9091:9091"
    environment:
      - "KEYCLOAK_ADMIN=admin"
      - "KEYCLOAK_ADMIN_PASSWORD=admin"
      - "KC_LOG_LEVEL=INFO"
      - "KC_DB=postgres"
      - "KC_DB_USERNAME=keycloak"
      - "KC_DB_PASSWORD=keycloak"
      - "KC_DB_SCHEMA=keycloak"
      - "KC_DB_URL_DATABASE=keycloak"
      - "KC_DB_URL_HOST=host.docker.internal"
    networks:
      - ${GLOBAL_NETWORK:-services}
