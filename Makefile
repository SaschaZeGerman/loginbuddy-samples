build_all:
	mvn clean install
	docker build --no-cache --tag saschazegerman/loginbuddy-democlient:latest -f Dockerfile_democlient .
	docker build --no-cache --tag saschazegerman/loginbuddy-demoserver:latest -f Dockerfile_demoserver .

initialize_dev:
	sh initialize-dev-environment.sh