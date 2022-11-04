#!/bin/bash

# This script may be used to generate an initial .env file.
# Once created, Loginbuddy will reuse this key until this script is run again.

# create directory to hold local files that are ignored from git
mkdir -p dev

# clean up a previously script execution
#
mv .env dev/.env.bak
rm -f .env

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

sh initialize-dev-tls-keypair.sh