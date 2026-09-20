.PHONY: build up down logs ps clean test-payments kafka-topics help

help:
	@echo "Wallet Backend — common targets:"
	@echo "  make build          - mvn clean package -DskipTests"
	@echo "  make up             - docker compose up -d"
	@echo "  make down           - docker compose down"
	@echo "  make logs           - docker compose logs -f"
	@echo "  make ps             - docker compose ps"
	@echo "  make clean          - mvn clean + docker compose down -v"
	@echo "  make test-payments  - curl POST /api/v1/payments with idempotency key"
	@echo "  make kafka-topics   - list Kafka topics"

build:
	mvn -s $$HOME/.m2/settings.local.xml clean package -DskipTests

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

clean:
	mvn -s $$HOME/.m2/settings.local.xml clean
	docker compose down -v

test-payments:
	@curl -i -X POST http://localhost:8080/api/v1/payments \
	  -H "Content-Type: application/json" \
	  -H "Idempotency-Key: demo-$$(date +%s)" \
	  -H "X-Correlation-Id: demo-corr-$$(date +%s)" \
	  -d '{"userId":"u-1","amount":"100.00 USD"}'

kafka-topics:
	docker exec -it wallet-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
