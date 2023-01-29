build_all:
	docker build --no-cache --tag local/loadbalancer:latest .
	docker build --no-cache --tag local/loadbalancer-http:latest -f Dockerfile_http .
	docker build --no-cache --tag local/loginbuddy-http:latest -f Dockerfile_loginbuddy_http .