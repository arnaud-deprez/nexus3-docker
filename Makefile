IMAGE_NAME = arnaudeprez/nexus3:latest

.PHONY: build
build:
	gradle transformScriptToJson
	docker build -t $(IMAGE_NAME) .

.PHONY: test
test: build
	#IMAGE_NAME=$(IMAGE_NAME) test/run

.PHONY: publish
publish: build test
	echo "$(DOCKER_PASSWORD)" | docker login -u "$(DOCKER_USERNAME)" --password-stdin
	docker push $(IMAGE_NAME)