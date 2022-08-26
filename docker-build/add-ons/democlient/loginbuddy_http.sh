#!/bin/bash

if [ -z "$DEMOCLIENT_LOCATION" ]
then
  printf "Using the default democlient location: https://democlient.loginbuddy.net\n"
  DEMOCLIENT_LOCATION=http://democlient.loginbuddy.net
fi

if [ -z "$DEMOCLIENT_HOSTNAME" ]
then
  printf "Using default hostname democlient.loginbuddy.net\n"
  DEMOCLIENT_HOSTNAME=democlient.loginbuddy.net
fi

# Find the policy file that contains socket permissions and add them to the default catalina.policy file
# default is located here: /usr/local/tomcat/conf/catalina.policy
#
cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/permissions.policy >> /usr/local/tomcat/conf/catalina.policy

# Check if hazelcast is used. In that case, add required permissions
#
if [ -z "$HAZELCAST" ]
then
  printf "Using local cache.\n"
else
  printf "Attempting to use remote cache with Hazelcast. Adding required permissions\n"
  cat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/hazelcastPermissions.policy >> /usr/local/tomcat/conf/catalina.policy
fi

# specifying that 'none' is invalid for JWT signature algorithms
#
export CATALINA_OPTS="${SYSTEM_PROPS} -Dorg.jose4j.jws.default-allow-none=false"

# replace @@variable@@ in server.xml with the real values
#
cp /opt/docker/server_http.xml /usr/local/tomcat/conf/server.xml
sed -i "s/@@hostname@@"/${DEMOCLIENT_HOSTNAME}/g /usr/local/tomcat/conf/server.xml

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security