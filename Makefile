build_all:
	mvn clean package
	docker build --no-cache --tag saschazegerman/loginbuddy-democlient:latest -f Dockerfile_democlient .
	docker build --no-cache --tag saschazegerman/loginbuddy-demoserver:latest -f Dockerfile_demoserver .