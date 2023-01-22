#!/bin/bash

# This script generates an initial keypair for development purposes.
# Once created, the Loginbuddy containers will reuse this key until this script is run again.

# Exporting environment variables for key creation
#
export $(cat .env | grep HOSTNAME_LOGINBUDDY)
export $(cat .env | grep DEMOCLIENT_HOSTNAME)
export $(cat .env | grep DEMOSERVER_HOSTNAME)
export $(cat .env | grep SSL_PWD)

# Remove an existing keypair,  a new one will be created
#
rm -f dev/loginbuddy.p12
rm -f dev/loginbuddy_client.p12
rm -f dev/loginbuddy_server.p12

# Create private key for LOGINBUDDY
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${SSL_PWD} \
  -keypass ${SSL_PWD} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${HOSTNAME_LOGINBUDDY}" \
  -ext san=dns:${HOSTNAME_LOGINBUDDY},dns:localhost

# Create private key for DEMOCLIENT
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy_client.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${SSL_PWD} \
  -keypass ${SSL_PWD} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${DEMOCLIENT_HOSTNAME}" \
  -ext san=dns:${DEMOCLIENT_HOSTNAME},dns:localhost

# Create private key for DEMOSERVER
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy_server.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${SSL_PWD} \
  -keypass ${SSL_PWD} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${DEMOSERVER_HOSTNAME}" \
  -ext san=dns:${DEMOSERVER_HOSTNAME},dns:localhost

# Remove all variables
#
unset HOSTNAME_LOGINBUDDY
unset DEMOCLIENT_HOSTNAME
unset DEMOSERVER_HOSTNAME
unset SSL_PWD