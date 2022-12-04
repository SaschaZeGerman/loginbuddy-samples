#!/bin/bash

if [ -z "$DEMOSERVER_LOCATION" ]
then
  printf "Using the default demoserver location https://demoserver.loginbuddy.net:8443\n"
  DEMOSERVER_LOCATION=https://demoserver.loginbuddy.net:8443
fi

if [ -z "$DEMOSERVER_HOSTNAME" ]
then
  printf "Using the default demoserver hostname demoserver.loginbuddy.net\n"
  DEMOSERVER_HOSTNAME=demoserver.loginbuddy.net
fi

if [ -z "$DEMOSERVER_SSL_PORT" ]
then
  printf "Using default SSL port 443\n"
  SSL_PORT=443
else
  SSL_PORT=$DEMOSERVER_SSL_PORT
fi

# creating a keystore and generating a password for it
#
UUID=${SSL_PWD}
if [ -z "$UUID" ]
then
  printf "Creating a TLS keystore including a password\n"
  # generating a UUID as password for the generated keystore
  #
  UUID=$(cat /proc/sys/kernel/random/uuid)
  # Create private key
  #
  keytool -genkey -alias loginbuddy -keystore /usr/local/tomcat/ssl/loginbuddy.p12 -storetype PKCS12 -keyalg RSA -storepass ${UUID} -keypass ${UUID} -validity 1 -keysize 2048 -dname "CN=${DEMOSERVER_HOSTNAME}" -ext san=dns:${DEMOSERVER_HOSTNAME},dns:loginbuddy-demoserver
else
  printf "Assuming a TLS keystore exists, none created! Do not forget to map your key as a volume to: '/usr/local/tomcat/ssl/loginbuddy.p12'!\n"
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
sed -i "s/@@hostname@@"/${DEMOSERVER_HOSTNAME}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslport@@"/${SSL_PORT}/g /usr/local/tomcat/conf/server.xml
sed -i "s/@@sslpwd@@"/${UUID}/g /usr/local/tomcat/conf/server.xml

# overwrite the variables since they are not needed anywhere anymore. For this demo we do not overwrite HOSTNAME_LOGINBUDDY!
#
unset SSL_PORT=
unset UUID=

# run the original tomcat entry point command as specified in tomcat's Dockerfile
#
sh /usr/local/tomcat/bin/catalina.sh run -security