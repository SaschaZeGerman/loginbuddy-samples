# Using nginx as loadbalancer to front two Loginbuddy instances

Nginx may be used as a load balancer in front of two or more Loginbuddy instances. This demo includes two possible configurations:

- 1: `[client] -- https --> [nginx] -- http --> [loginbuddy]`
- 2: `[client] -- https --> [nginx] -- https --> [loginbuddy]`

## Configuration 1

Update the **nginx_ssl.conf** to your needs. By default, it uses these values:

- **Loginbuddy**
  - Loginbuddy listening on http port 8080 which is not exposed externally and does not forward to SSL
- **Loadbalancer**
  - listens on SSL port 8444 (which requires no updates of the samples project)
  - proxies requests (round robin) to any Loginbuddy instance as configured in nginx_ssl.conf

## Configuration 2

Update the **nginx_ssl_ssl.conf** to your needs. By default, it uses these values:

- **Loginbuddy**
  - Loginbuddy listening on https port 443 which is not exposed externally and does not forward to SSL
- **Loadbalancer**
  - listens on SSL port 8444 (which requires no updates of the samples project)
  - proxies requests (round robin) to any Loginbuddy instance as configured in nginx_ssl.conf

## Loadbalancer key and cert for inbound SSL connections

Find a discussion on non-interactive cert creation here:

- [non-interactive cert creation](https://serverfault.com/questions/649990/non-interactive-creation-of-ssl-certificate-requests)
- example for subj: `-subj "/C=PE/ST=Lima/L=Lima/O=Acme Inc. /OU=IT Department/CN=acme.com"`

For development purposes, run this command:

- `openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout loadbalancer.key -out loadbalancer.crt -subj "/CN=local.loginbuddy.net"`

## Build the loadbalancer and Loginbuddy

- `make build_all`

This will produce new images

- **local/loadbalancer-http:latest**
  - this is a nginx loadbalancer that uses configuration 1 (http -- http)
- **local/loadbalancer:latest**
  - this is a nginx loadbalancer that uses configuration 2 (https -- https)
- **local/loginbuddy-http:latest**
  - this is Loginbuddy, using configurations required to be used in a cluster via http
  - *web.xml* does not include security directions
  - *server.xml* opens port 8080 which is http only and does not forward to any SSL ports
  - *logging.properties* set to loglevel INFO

## Cluster setup

- relying parties (clients) connect to **https://local.loginbuddy.net:8444** (which is the load balancer)
- loadbalancer connects to **loginbuddy1:8080**, **loginbuddy2:8080**. Both have *local.loginbuddy.net* configured as hostname
  - with configuration 2 the loadbalancer connects to **loginbuddy1:443**, **loginbuddy2:443**
- loginbuddy1, loginbuddy2 connect to **hazelcast1:5701**, **hazelcast2:5701** which is a hazelcast cluster
- loginbuddy1, loginbuddy2 connect to OpenID Connect providers
- OpenID Connect providers redirects back to loadbalancer