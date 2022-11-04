#!/bin/bash

# Import the own generated keypair as trust anchor
#
# Export the public certificates
#
keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/demosetup.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass ${SSL_PWD}

# Import the cert as trusted certificate. Otherwise Loginbuddy could not connect to demoserver.loginbuddy.net
#
keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/demosetup.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

sh /opt/docker/loginbuddy.sh