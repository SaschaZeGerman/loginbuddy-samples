#!/bin/bash

# This script may be used to generate an initial private key for development purposes.
# Once created, Loginbuddy will reuse this key until this script is run again.

# create directory to hold local files that are ignored from git
mkdir -p dev

# clean up a previously script execution
#
mv .env dev/.env.bak
mv dev/loginbuddy.p12 dev/loginbuddy.p12.bak

rm -f .env
rm -f dev/loginbuddy.p12

# create the private keys secret
#
secret=$(openssl rand -base64 32 | tr -d '=' | tr -d '/' | tr -d '+')

cp templates/env_template .env

# Exporting hostnames as environment variables
#
export $(cat .env | grep HOSTNAME_LOGINBUDDY)
export $(cat .env | grep DEMOCLIENT_HOSTNAME)
export $(cat .env | grep DEMOSERVER_HOSTNAME)
printf "\nSSL_PWD=${secret}" >> .env

# Create private key
#
keytool -genkey \
  -alias loginbuddy \
  -keystore dev/loginbuddy.p12 \
  -storetype PKCS12 \
  -keyalg RSA -storepass ${secret} \
  -keypass ${secret} \
  -validity 365 \
  -keysize 2048 \
  -dname "CN=${HOSTNAME_LOGINBUDDY}" \
  -ext san=dns:${HOSTNAME_LOGINBUDDY},dns:${DEMOSERVER_HOSTNAME},dns:${DEMOCLIENT_HOSTNAME},dns:localhost