#!/bin/bash

# Import the own certificate as trust anchor
#
# Export the public certificates
#
keytool -export -alias loginbuddy -file /usr/local/tomcat/ssl/demosetup.crt -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storepass ${SSL_PWD}

# Import the certs as trusted certificates
#
keytool -importcert -alias loginbuddy -file /usr/local/tomcat/ssl/demosetup.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts -trustcacerts -noprompt

sh /opt/docker/loginbuddy.sh