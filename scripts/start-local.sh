#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

# Ensure SDKMAN Java is on PATH if system java is missing
if ! command -v java &>/dev/null || ! java -version &>/dev/null 2>&1; then
  if [[ -d "$HOME/.sdkman/candidates/java/current/bin" ]]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

# --- Flags ---
SKIP_BUILD=false
SKIP_INFRA=false
NO_FRONTEND=false
NO_AOT=false

for arg in "$@"; do
  case "$arg" in
    --skip-build)  SKIP_BUILD=true ;;
    --skip-infra)  SKIP_INFRA=true ;;
    --no-frontend) NO_FRONTEND=true ;;
    --no-aot)      NO_AOT=true ;;
    -h|--help)
      echo "Usage: $0 [--skip-build] [--skip-infra] [--no-frontend] [--no-aot]"
      echo "  --skip-build   Skip Gradle builds"
      echo "  --skip-infra   Skip starting Docker infrastructure"
      echo "  --no-frontend  Skip starting the frontend dev server"
      echo "  --no-aot       Run services from the fat JAR without the Java 25 AOT cache"
      exit 0
      ;;
    *)
      echo "Unknown flag: $arg"
      exit 1
      ;;
  esac
done

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $1"; }

# --- Prerequisites ---
check_prereqs() {
  info "Checking prerequisites..."
  local missing=false

  if ! command -v java &>/dev/null; then
    fail "Java not found. Install Java 25."
    missing=true
  else
    local java_ver
    java_ver=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)[.".].*/\1/')
    # Bytecode targets Java 25 and the bundled GraalVM polyglot runtime is built for JDK 25
    if [[ "$java_ver" -ne 25 ]]; then
      fail "Java 25 required (found $java_ver). Set JAVA_HOME to a JDK 25."
      missing=true
    else
      ok "Java $java_ver"
    fi
  fi

  if ! command -v node &>/dev/null; then
    if [[ "$NO_FRONTEND" == false ]]; then
      fail "Node.js not found. Install Node.js 18+ or use --no-frontend."
      missing=true
    else
      warn "Node.js not found (skipped, --no-frontend)"
    fi
  else
    local node_ver
    node_ver=$(node -v | sed 's/v\([0-9]*\).*/\1/')
    if [[ "$node_ver" -lt 18 ]]; then
      fail "Node.js 18+ required (found $node_ver)."
      missing=true
    else
      ok "Node.js v$(node -v | sed 's/v//')"
    fi
  fi

  if ! command -v docker &>/dev/null; then
    if [[ "$SKIP_INFRA" == false ]]; then
      fail "Docker not found. Install Docker or use --skip-infra."
      missing=true
    else
      warn "Docker not found (skipped, --skip-infra)"
    fi
  else
    ok "Docker $(docker --version | sed 's/Docker version \([^,]*\).*/\1/')"
  fi

  if [[ "$missing" == true ]]; then
    fail "Missing prerequisites. Aborting."
    exit 1
  fi
}

# --- PID management ---
PIDS=()

save_pid() {
  local name=$1 pid=$2
  echo "$pid" > "$LOGS_DIR/${name}.pid"
  PIDS+=("$pid")
}

cleanup() {
  echo ""
  warn "Shutting down all services..."
  for pidfile in "$LOGS_DIR"/*.pid; do
    [[ -f "$pidfile" ]] || continue
    local pid
    pid=$(<"$pidfile")
    local name
    name=$(basename "$pidfile" .pid)
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      info "Sent SIGTERM to $name (PID $pid)"
    fi
    rm -f "$pidfile"
  done
  # Give processes time to stop
  sleep 2
  for pid in "${PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
  ok "All services stopped."
  exit 0
}

trap cleanup SIGINT SIGTERM

# --- Health check helpers ---
wait_for_pg() {
  info "Waiting for PostgreSQL..."
  local retries=30
  while [[ $retries -gt 0 ]]; do
    if (command -v pg_isready &>/dev/null && pg_isready -h localhost -p 5432 -U ruleforge &>/dev/null) || \
       docker exec docker-postgres-1 pg_isready -U ruleforge &>/dev/null; then
      ok "PostgreSQL is ready"
      return 0
    fi
    retries=$((retries - 1))
    sleep 2
  done
  fail "PostgreSQL did not become ready"
  exit 1
}

wait_for_redis() {
  info "Waiting for Redis..."
  local retries=30
  while [[ $retries -gt 0 ]]; do
    if (command -v redis-cli &>/dev/null && redis-cli -h localhost -p 6379 ping 2>/dev/null | grep -q PONG) || \
       docker exec docker-redis-1 redis-cli ping 2>/dev/null | grep -q PONG; then
      ok "Redis is ready"
      return 0
    fi
    retries=$((retries - 1))
    sleep 2
  done
  fail "Redis did not become ready"
  exit 1
}

wait_for_kafka() {
  info "Waiting for Kafka..."
  local retries=30
  while [[ $retries -gt 0 ]]; do
    if docker exec "$(docker ps -q -f ancestor=apache/kafka:3.7.1 2>/dev/null)" \
         /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 &>/dev/null 2>&1; then
      ok "Kafka is ready"
      return 0
    fi
    # Fallback: try connecting with nc
    if nc -z localhost 9092 2>/dev/null; then
      sleep 3
      ok "Kafka port open (broker likely ready)"
      return 0
    fi
    retries=$((retries - 1))
    sleep 3
  done
  fail "Kafka did not become ready"
  exit 1
}

wait_for_elasticsearch() {
  info "Waiting for Elasticsearch..."
  local retries=30
  while [[ $retries -gt 0 ]]; do
    if curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -qE '"status":"(green|yellow)"'; then
      ok "Elasticsearch is ready"
      return 0
    fi
    retries=$((retries - 1))
    sleep 3
  done
  fail "Elasticsearch did not become ready"
  exit 1
}

wait_for_minio() {
  info "Waiting for MinIO..."
  local retries=20
  while [[ $retries -gt 0 ]]; do
    if curl -s http://localhost:9000/minio/health/live &>/dev/null; then
      ok "MinIO is ready"
      return 0
    fi
    retries=$((retries - 1))
    sleep 2
  done
  fail "MinIO did not become ready"
  exit 1
}

wait_for_http() {
  local name=$1 port=$2 path=${3:-/actuator/health}
  local retries=40
  info "Waiting for $name on port $port..."
  while [[ $retries -gt 0 ]]; do
    if curl -sf "http://localhost:${port}${path}" &>/dev/null; then
      ok "$name is healthy"
      return 0
    fi
    retries=$((retries - 1))
    sleep 3
  done
  fail "$name did not become healthy on port $port"
  return 1
}

# --- Run database migrations ---
run_migrations() {
  info "Running database migrations..."

  if command -v flyway &>/dev/null; then
    # Flyway CLI available — use it (tracks state, safe to re-run)
    (cd "$PROJECT_ROOT/db" && flyway -configFiles=flyway.conf migrate)
    ok "Migrations applied via Flyway"
  else
    # No Flyway CLI — apply SQL files in order via psql through Docker
    # Uses a simple tracking table to skip already-applied migrations
    docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -c "
      CREATE TABLE IF NOT EXISTS _migration_log (
        filename TEXT PRIMARY KEY,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );" &>/dev/null

    local count=0
    for sql_file in $(ls "$PROJECT_ROOT/db/migrations"/V*.sql | sort); do
      local filename
      filename=$(basename "$sql_file")
      local already_applied
      already_applied=$(docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -tAq \
        -c "SELECT COUNT(*) FROM _migration_log WHERE filename = '$filename';" 2>/dev/null)
      if [[ "$already_applied" == "1" ]]; then
        continue
      fi
      docker exec -i docker-postgres-1 psql -U ruleforge -d ruleforge \
        < "$sql_file" &>/dev/null && \
        docker exec docker-postgres-1 psql -U ruleforge -d ruleforge -q \
          -c "INSERT INTO _migration_log(filename) VALUES ('$filename') ON CONFLICT DO NOTHING;" \
          &>/dev/null
      count=$((count + 1))
    done

    if [[ $count -gt 0 ]]; then
      ok "Applied $count migration(s) via psql"
    else
      ok "No new migrations to apply"
    fi
  fi
}

# --- Start a platform service ---
start_service() {
  local name=$1 port=$2
  local jar
  jar=$(ls "$PROJECT_ROOT/platform/${name}/build/libs/${name}"-*.jar 2>/dev/null | grep -v plain | head -1)
  if [[ -z "$jar" ]]; then
    fail "No JAR found for $name. Did the build succeed?"
    return 1
  fi
  info "Starting $name (port $port)..."

  # Java 25: compact object headers (JEP 519) shrink every object header from 12 to 8 bytes
  local jvm_opts=(-XX:+UseCompactObjectHeaders)

  if [[ "$NO_AOT" == true ]]; then
    java "${jvm_opts[@]}" -jar "$jar" \
      --spring.profiles.active=local \
      --server.port="$port" \
      > "$LOGS_DIR/${name}.log" 2>&1 &
  else
    # Java 25 AOT cache (JEPs 483/514/515): needs the extracted layout, not the nested fat JAR.
    # Trained once per build, then reused, so later starts skip most class loading and linking.
    local aot_dir="$PROJECT_ROOT/platform/${name}/build/aot"
    local app_jar
    app_jar="$aot_dir/app/$(basename "$jar")"
    (
      if [[ ! -f "$aot_dir/app.aot" || "$jar" -nt "$aot_dir/app.aot" ]]; then
        rm -rf "$aot_dir" && mkdir -p "$aot_dir"
        java -Djarmode=tools -jar "$jar" extract --destination "$aot_dir/app" > "$LOGS_DIR/${name}.aot.log" 2>&1
        # Training run: start the context, record what was loaded, exit
        java "${jvm_opts[@]}" -XX:AOTCacheOutput="$aot_dir/app.aot" -Dspring.context.exit=onRefresh \
          -jar "$app_jar" --spring.profiles.active=local --server.port=0 >> "$LOGS_DIR/${name}.aot.log" 2>&1 \
          || rm -f "$aot_dir/app.aot"
      fi
      if [[ -f "$aot_dir/app.aot" ]]; then
        exec java "${jvm_opts[@]}" -XX:AOTCache="$aot_dir/app.aot" -jar "$app_jar" \
          --spring.profiles.active=local --server.port="$port"
      fi
      # Training failed: fall back to the fat JAR
      exec java "${jvm_opts[@]}" -jar "$jar" --spring.profiles.active=local --server.port="$port"
    ) > "$LOGS_DIR/${name}.log" 2>&1 &
  fi
  save_pid "$name" $!
}

# --- Main ---
main() {
  mkdir -p "$LOGS_DIR"

  echo ""
  echo -e "${CYAN}========================================${NC}"
  echo -e "${CYAN}   RuleForge Local Development Stack${NC}"
  echo -e "${CYAN}========================================${NC}"
  echo ""

  check_prereqs

  # --- Infrastructure ---
  if [[ "$SKIP_INFRA" == false ]]; then
    info "Starting infrastructure containers..."
    docker compose -f "$PROJECT_ROOT/infra/docker/docker-compose.infra.yml" up -d
    wait_for_pg
    wait_for_redis
    wait_for_kafka
    wait_for_elasticsearch
    wait_for_minio
  else
    warn "Skipping infrastructure (--skip-infra)"
    wait_for_pg
  fi

  # --- Migrations ---
  run_migrations

  # --- Build ---
  if [[ "$SKIP_BUILD" == false ]]; then
    info "Building engine..."
    (cd "$PROJECT_ROOT/engine" && ./gradlew build -x test -x jacocoTestCoverageVerification)
    ok "Engine build complete"

    info "Building platform..."
    (cd "$PROJECT_ROOT/platform" && ./gradlew build -x test)
    ok "Platform build complete"

    if [[ "$NO_FRONTEND" == false ]]; then
      info "Installing frontend dependencies..."
      (cd "$PROJECT_ROOT/frontend" && npm install)
      ok "Frontend dependencies installed"
    fi

    if command -v python3 &>/dev/null && [[ -f "$PROJECT_ROOT/ai-service/requirements.txt" ]]; then
      info "Installing AI service dependencies..."
      (cd "$PROJECT_ROOT/ai-service" \
        && python3 -m venv .venv \
        && .venv/bin/pip install -r requirements.txt -q)
      ok "AI service dependencies installed"
    fi
  else
    warn "Skipping builds (--skip-build)"
  fi

  # --- Start services ---
  info "Starting platform services..."

  # Phase 1: auth-service first (others depend on it)
  start_service "ruleforge-auth-service" 8081
  wait_for_http "ruleforge-auth-service" 8081

  # Phase 2: independent services in parallel
  start_service "ruleforge-management-service" 8082
  start_service "ruleforge-event-service" 8084
  start_service "ruleforge-connector-service" 8087

  # Phase 3: solver depends on auth
  start_service "ruleforge-solver-service" 8083

  # Wait for parallel services
  wait_for_http "ruleforge-management-service" 8082 || true
  wait_for_http "ruleforge-event-service" 8084 || true
  wait_for_http "ruleforge-connector-service" 8087 || true
  wait_for_http "ruleforge-solver-service" 8083 || true

  # Phase 4: analytics depends on Kafka + ES
  start_service "ruleforge-analytics-service" 8085
  wait_for_http "ruleforge-analytics-service" 8085 || true

  # Phase 5: scheduler depends on solver
  start_service "ruleforge-scheduler-service" 8086
  wait_for_http "ruleforge-scheduler-service" 8086 || true

  # --- AI Service (optional) ---
  if command -v python3 &>/dev/null && [[ -f "$PROJECT_ROOT/ai-service/requirements.txt" ]]; then
    info "Starting AI service..."
    (cd "$PROJECT_ROOT/ai-service" && .venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 \
      > "$LOGS_DIR/ai-service.log" 2>&1) &
    save_pid "ai-service" $!
    wait_for_http "ai-service" 8000 "/health" || true
  else
    warn "Skipping AI service (Python not found or ai-service not present)"
  fi

  # --- Frontend ---
  if [[ "$NO_FRONTEND" == false ]]; then
    info "Starting frontend dev server..."
    (cd "$PROJECT_ROOT/frontend" && npm run dev > "$LOGS_DIR/frontend.log" 2>&1) &
    save_pid "frontend" $!
    sleep 3
    ok "Frontend dev server started"
  else
    warn "Skipping frontend (--no-frontend)"
  fi

  # --- Status table ---
  echo ""
  echo -e "${CYAN}========================================${NC}"
  echo -e "${CYAN}   Service Status${NC}"
  echo -e "${CYAN}========================================${NC}"
  printf "${CYAN}%-22s %-6s %-8s %-10s${NC}\n" "SERVICE" "PORT" "PID" "STATUS"
  echo "----------------------------------------------"

  for pidfile in "$LOGS_DIR"/*.pid; do
    [[ -f "$pidfile" ]] || continue
    local pid name port status
    pid=$(<"$pidfile")
    name=$(basename "$pidfile" .pid)
    case "$name" in
      ruleforge-auth-service)       port=8081 ;;
      ruleforge-management-service) port=8082 ;;
      ruleforge-solver-service)     port=8083 ;;
      ruleforge-event-service)      port=8084 ;;
      ruleforge-analytics-service)  port=8085 ;;
      ruleforge-scheduler-service)  port=8086 ;;
      ruleforge-connector-service)  port=8087 ;;
      ai-service)         port=8000 ;;
      frontend)           port=3000 ;;
      *)                  port="----" ;;
    esac
    if kill -0 "$pid" 2>/dev/null; then
      status="${GREEN}running${NC}"
    else
      status="${RED}stopped${NC}"
    fi
    printf "%-22s %-6s %-8s %b\n" "$name" "$port" "$pid" "$status"
  done

  echo ""
  ok "RuleForge stack is running. Press Ctrl+C to stop all services."
  echo -e "  Logs directory: ${CYAN}$LOGS_DIR/${NC}"
  echo ""

  # Wait for all background processes
  wait
}

main
