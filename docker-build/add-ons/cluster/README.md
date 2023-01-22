# Using nginx as loadbalancer to front two Loginbuddy instances

## Configuration

Update the **nginx_ssl.conf** to your needs. By default, it uses these values:

- **Loginbuddy**
  - Loginbuddy listening on http port 8080 which is not exposed externally and does not forward to SSL
- **Loadbalancer**
  - listens on SSL port 8444 (which requires no updates of the samples project to make it work with the cluster)
  - this forward requests (round robin) to any Loginbuddy instance as configured in nginx_ssl.conf

## Key and Cert for inbound SSL connections

Find a discussion on non-interactive cert creation here:

- [non-interactive cert creation](https://serverfault.com/questions/649990/non-interactive-creation-of-ssl-certificate-requests)
- example for subj: `-subj "/C=PE/ST=Lima/L=Lima/O=Acme Inc. /OU=IT Department/CN=acme.com"`

For development purposes, start with this:

- `openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout loadbalancer.key -out loadbalancer.crt -subj "/CN=local.loginbuddy.net"`

## Build the loadbalancer and Loginbuddy

- `make build_all`

This will produce new images

- **local/loadbalancer:latest**
  - this is a nginx loadbalancer
- **local/loginbuddy-cluster:latest**
  - this is Loginbuddy, using configurations required to be used in a cluster via http
  - *web.xml* does not include security directions
  - *server.xml* includes port 8080 which is http only and does not forward to any SSL ports
  - *logging.properties* set to loglevel WARNING

## Cluster setup

- relying parties (clients) connect to **local.loginbuddy.net:8444** (which is the load balancer)
- loadbalancer connects to **loginbuddy1:8080**, **loginbuddy2:8080**. Both have *local.loginbuddy.net* configured as hostname
- loginbuddy1, loginbuddy2 connect to **hazelcast1:5701**, **hazelcast2:5701** which is a hazelcast cluster
- loginbuddy1, loginbuddy2 connecto to OpenID Connect providers
- OpenID Connect providers redirect back to loadbalancer