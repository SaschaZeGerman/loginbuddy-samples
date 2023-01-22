FROM nginx:latest

COPY nginx_ssl.conf /etc/nginx/nginx.conf
COPY loadbalancer.crt /etc/nginx/ssl.crt
COPY loadbalancer.key /etc/nginx/ssl.key