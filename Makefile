.PHONY: help infra-up infra-down build-engine build-platform test-engine test-platform test-frontend stack-up stack-down migrate dev-frontend dev-up dev-down dev-up-fast

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

infra-up: ## Start infrastructure (PG, Redis, Kafka, ES, MinIO)
	docker compose -f infra/docker/docker-compose.infra.yml up -d

infra-down: ## Stop infrastructure
	docker compose -f infra/docker/docker-compose.infra.yml down

build-engine: ## Build engine modules
	cd engine && ./gradlew build

build-platform: ## Build platform services
	cd platform && ./gradlew build -x test

test-engine: ## Run engine tests
	cd engine && ./gradlew test

test-platform: ## Run platform tests
	cd platform && ./gradlew test

test-frontend: ## Run frontend tests
	cd frontend && npm test

stack-up: ## Start full stack (all services + infra)
	docker compose -f infra/docker/docker-compose.yml up -d --build

stack-down: ## Stop full stack
	docker compose -f infra/docker/docker-compose.yml down

migrate: ## Run database migrations
	cd db && flyway -configFiles=flyway.conf migrate

dev-frontend: ## Start frontend dev server
	cd frontend && npm run dev

lint-frontend: ## Lint frontend code
	cd frontend && npm run lint

dev-up: ## Start full local dev stack (build + infra + services + frontend)
	./scripts/start-local.sh

dev-down: ## Stop all local dev services
	./scripts/stop-local.sh

dev-up-fast: ## Start local dev stack without rebuilding
	./scripts/start-local.sh --skip-build
