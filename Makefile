IMAGE_NAME = arnaudeprez/nexus3:latest

buildCharts:
	for chart in charts/*; do \
		helm dependency build $$chart; \
		helm lint $$chart; \
	done

.PHONY: build
build: buildCharts
	gradle transformScriptToJson
	docker build -t $(IMAGE_NAME) .

.PHONY: test
test: build
	#IMAGE_NAME=$(IMAGE_NAME) test/run

.PHONY: publish
publish: build test
	echo "$(DOCKER_PASSWORD)" | docker login -u "$(DOCKER_USERNAME)" --password-stdin
	docker push $(IMAGE_NAME)