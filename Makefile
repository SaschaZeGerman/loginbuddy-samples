define BUILD_DOCKER
	docker build --no-cache --tag saschazegerman/loginbuddy-democlient:latest -f Dockerfile_democlient .
	docker build --no-cache --tag saschazegerman/loginbuddy-demoserver:latest -f Dockerfile_demoserver .
endef

# Compile the code and build the docker images
#
build_all:
	mvn clean package
	$(BUILD_DOCKER)

# Compile the code and build docker images using the builder image
# Use this target if you do not have Java and Maven installed
# Run the target 'build_all_non_dev' of the repository 'loginbuddy-tools' before running this target
# https://github.com/SaschaZeGerman/loginbuddy-tools
#
build_all_non_dev:
	docker run -v `pwd`:/tmp saschazegerman/loginbuddy-tools:latest mvn -f "/tmp/pom.xml" clean package
	$(BUILD_DOCKER)