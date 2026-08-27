NUM_GAMES ?= 5000
ALPHA ?= 1.0
RUN_NAME ?= run-$(shell date +%Y%m%d-%H%M%S)

selfplay:
	./gradlew run --args="selfplay --games=$(NUM_GAMES) --out=data"

train:
	cd training && python train.py --data-dir=../data --out=../weights/weights_latest.json \
		--alpha=$(ALPHA) --run-name=$(RUN_NAME)

benchmark:
	./gradlew run --args="benchmark --v1=weights/weights_v1.json --v2=weights/weights_latest.json"

iterate: selfplay train benchmark
	@echo "Review benchmark results before committing weights_latest.json"

mlflow-ui:
	cd training && mlflow ui