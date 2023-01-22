# Using nginx as loadbalancer to front two Loginbuddy instances

## Configuration

Update the **nginx_ssl.conf** to your needs. By default, it uses these values:

- **Loginbuddy**: proxying to two Loginbuddy instances using http on port 8080
- **Loadbalancer**: listens on port 8444 (which requires no updates of the samples project to make it work with the cluster)

## Key and Cert for inbound SSL connections

Find a discussion on non-interactive cert creation here:

- [non-interactive cert creation](https://serverfault.com/questions/649990/non-interactive-creation-of-ssl-certificate-requests)
- example for subj: `-subj "/C=PE/ST=Lima/L=Lima/O=Acme Inc. /OU=IT Department/CN=acme.com"`

For development purposes, start with this:

- `openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout loadbalancer.key -out loadbalancer.crt -subj "/CN=local.loginbuddy.net"`

## Build the loadbalancer

- `make build_all`

This will produce new images

- **local/loadbalancer:latest**
  - this is a nginx loadbalancer
- **local/loginbuddy-cluster:latest**
  - this is Loginbuddy, using configurations required to be used in a cluster

