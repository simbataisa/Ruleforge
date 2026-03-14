.PHONY: help infra-up infra-down build-engine build-platform test-engine test-platform test-frontend stack-up stack-down migrate dev-frontend dev-up dev-down dev-up-fast dev-status dev-logs seed-analytics seed-audit smoke-test

CYAN  := \033[0;36m
GREEN := \033[0;32m
RED   := \033[0;31m
YELLOW:= \033[1;33m
BOLD  := \033[1m
NC    := \033[0m

help: ## Show this help with service URLs
	@echo ""
	@printf "$(BOLD)$(CYAN)RuleForge — available targets$(NC)\n"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-22s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@printf "$(BOLD)Service URLs (local dev):$(NC)\n"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:3000\n"  "Frontend"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8081\n"  "auth-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8082\n"  "management-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8083\n"  "solver-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8084\n"  "event-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8085\n"  "analytics-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8086\n"  "scheduler-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8087\n"  "connector-service"
	@printf "  $(CYAN)%-22s$(NC) http://localhost:8000\n"  "ai-service (optional)"
	@echo ""

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

dev-status: ## Show live health status for all services and infra
	@echo ""
	@printf "$(BOLD)$(CYAN)╔══════════════════════════════════════════════════╗$(NC)\n"
	@printf "$(BOLD)$(CYAN)║        RuleForge — Service Health Status         ║$(NC)\n"
	@printf "$(BOLD)$(CYAN)╚══════════════════════════════════════════════════╝$(NC)\n"
	@echo ""
	@printf "$(BOLD)  %-26s %-8s %-10s %s$(NC)\n" "SERVICE" "PORT" "STATUS" "URL"
	@printf "  %s\n" "──────────────────────────────────────────────────────────────"
	@$(MAKE) --no-print-directory _check_frontend
	@$(MAKE) --no-print-directory _check_svc SVC="auth-service"        PORT=8081 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="management-service"  PORT=8082 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="solver-service"      PORT=8083 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="event-service"       PORT=8084 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="analytics-service"   PORT=8085 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="scheduler-service"   PORT=8086 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="connector-service"   PORT=8087 PATH_=/actuator/health
	@$(MAKE) --no-print-directory _check_svc SVC="ai-service"          PORT=8000 PATH_=/health
	@printf "  %s\n" "──────────────────────────────────────────────────────────────"
	@echo ""
	@printf "$(BOLD)  Infrastructure:$(NC)\n"
	@$(MAKE) --no-print-directory _check_infra NAME="PostgreSQL" CMD="docker exec docker-postgres-1 pg_isready -U ruleforge -q"
	@$(MAKE) --no-print-directory _check_infra NAME="Redis      " CMD="docker exec docker-redis-1 redis-cli ping"
	@$(MAKE) --no-print-directory _check_infra NAME="Kafka      " CMD="nc -z localhost 9092"
	@$(MAKE) --no-print-directory _check_infra NAME="Elasticsearch" CMD='curl -sf http://localhost:9200/_cluster/health'
	@$(MAKE) --no-print-directory _check_infra NAME="MinIO      " CMD="curl -sf http://localhost:9000/minio/health/live"
	@echo ""

_check_frontend:
	@found_port=""; \
	for p in 3000 3001 3002 3003 3004 3005 3010 3020 5173; do \
		if curl -sf --max-time 1 "http://localhost:$$p/" -o /dev/null 2>/dev/null; then \
			found_port=$$p; break; \
		fi; \
	done; \
	if [ -n "$$found_port" ]; then \
		printf "  $(GREEN)%-26s %-8s %-10s$(NC) http://localhost:$$found_port\n" "Frontend" "$$found_port" "UP"; \
	else \
		printf "  $(RED)%-26s %-8s %-10s$(NC)\n" "Frontend" "3000" "DOWN"; \
	fi

_check_svc:
	@result=$$(curl -sf --max-time 2 "http://localhost:$(PORT)$(PATH_)" 2>/dev/null); \
	if [ $$? -eq 0 ]; then \
		printf "  $(GREEN)%-26s %-8s %-10s$(NC) http://localhost:$(PORT)\n" "$(SVC)" "$(PORT)" "UP"; \
	else \
		printf "  $(RED)%-26s %-8s %-10s$(NC)\n" "$(SVC)" "$(PORT)" "DOWN"; \
	fi

_check_infra:
	@if $(CMD) >/dev/null 2>&1; then \
		printf "  $(GREEN)✔$(NC)  %s\n" "$(NAME)"; \
	else \
		printf "  $(RED)✘$(NC)  %s\n" "$(NAME)"; \
	fi

seed-analytics: ## Seed Elasticsearch with 500 sample decision log documents
	./scripts/seed-analytics.sh

seed-analytics-clear: ## Clear and re-seed Elasticsearch analytics data
	./scripts/seed-analytics.sh --clear

seed-audit: ## Seed PostgreSQL with comprehensive audit event data (100+ events)
	docker exec -i docker-postgres-1 psql -U ruleforge -d ruleforge < db/migrations/V046__seed_audit_events_comprehensive.sql

smoke-test: ## Run smoke tests against local dev stack
	./scripts/smoke-test.sh

dev-logs: ## Tail logs for a service (usage: make dev-logs SVC=ruleforge-auth-service)
	@if [ -z "$(SVC)" ]; then \
		echo "Usage: make dev-logs SVC=<service-name>"; \
		echo "Available logs:"; \
		ls logs/*.log 2>/dev/null | xargs -n1 basename | sed 's/\.log//' | sed 's/^/  /'; \
	else \
		tail -f logs/$(SVC).log; \
	fi
