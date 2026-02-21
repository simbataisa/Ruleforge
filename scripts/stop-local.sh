#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/logs"

# --- Flags ---
INCLUDE_INFRA=false

for arg in "$@"; do
  case "$arg" in
    --include-infra) INCLUDE_INFRA=true ;;
    -h|--help)
      echo "Usage: $0 [--include-infra]"
      echo "  --include-infra  Also stop Docker infrastructure containers"
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

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}   Stopping RuleForge Services${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# --- Stop services via PID files ---
stopped=0
if [[ -d "$LOGS_DIR" ]]; then
  for pidfile in "$LOGS_DIR"/*.pid; do
    [[ -f "$pidfile" ]] || continue
    pid=$(<"$pidfile")
    name=$(basename "$pidfile" .pid)

    if kill -0 "$pid" 2>/dev/null; then
      info "Stopping $name (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      stopped=$((stopped + 1))
    else
      warn "$name (PID $pid) already stopped"
    fi
    rm -f "$pidfile"
  done

  # Wait for graceful shutdown
  if [[ $stopped -gt 0 ]]; then
    info "Waiting 5 seconds for graceful shutdown..."
    sleep 5

    # Force-kill any remaining
    for pidfile in "$LOGS_DIR"/*.pid; do
      [[ -f "$pidfile" ]] || continue
      pid=$(<"$pidfile")
      name=$(basename "$pidfile" .pid)
      if kill -0 "$pid" 2>/dev/null; then
        warn "Force-killing $name (PID $pid)"
        kill -9 "$pid" 2>/dev/null || true
      fi
      rm -f "$pidfile"
    done
  fi
else
  warn "No logs directory found. No services to stop."
fi

ok "Stopped $stopped service(s)."

# --- Infrastructure ---
if [[ "$INCLUDE_INFRA" == true ]]; then
  info "Stopping infrastructure containers..."
  docker compose -f "$PROJECT_ROOT/infra/docker/docker-compose.infra.yml" down
  ok "Infrastructure stopped."
else
  info "Infrastructure containers left running. Use --include-infra to stop them."
fi

echo ""
ok "Done."
