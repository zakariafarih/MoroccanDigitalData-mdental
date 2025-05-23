#!/usr/bin/env bash
# =============================================================================
# MDental Platform Launcher
#
# A comprehensive launcher for the MDental micro-services platform.
# Provides a terminal-based UI (dialog) for configuring and launching services.
#
# Dependencies:
#   • dialog      (apt-get install dialog)
#   • docker      (for infrastructure services)
#   • Java 17+
# =============================================================================

set -eo pipefail

# Never inherit sensitive vars into child processes that could be ps-listed
export -n SUPER_ADMIN_PASS SUPER_ADMIN_TOKEN

# -----------------------------------------------------------------------------
# Script & Directory Layout
# -----------------------------------------------------------------------------
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONFIG_DIR="$SCRIPT_DIR/config"
LOGS_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOGS_DIR/auth-core"

# -----------------------------------------------------------------------------
# Default Configuration Values
# -----------------------------------------------------------------------------
POSTGRES_PORT=5432
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="postgres"

KEYCLOAK_PORT=9080
KEYCLOAK_ADMIN="admin"
KEYCLOAK_ADMIN_PASSWORD="admin"

# Added for automated Keycloak login / realm creation
KEYCLOAK_URL="http://localhost:${KEYCLOAK_PORT}"

# —— service-account client in *platform* realm ————————————————
# created below in platform-realm.json and owns the SUPER_ADMIN role
PLATFORM_REALM_CLIENT_ID="mdental-platform-admin"
PLATFORM_REALM_CLIENT_SECRET="bRFrYYJ9HUyYonPWc9wG2eDQ1OO2KN0l"

SUPER_ADMIN_TOKEN=""                   # populated at runtime – never persisted
AUTH_CORE_URL="http://localhost:8081"  # auth-core base URL

# Docker network
DOCKER_NETWORK="platform-net"

# Service configuration with correct ordering (discovery-server first)
SERVICES=(
  "discovery-server:8761"  # Eureka service
  "gateway-core:8080"
  "auth-core:8081"
  "clinic-core:8082"
  "patient-core:8083"
)

# Default launch mode for each service (all "ide" by default)
declare -A SERVICE_LAUNCH_MODES
for service_combo in "${SERVICES[@]}"; do
  service_name="${service_combo%%:*}"
  SERVICE_LAUNCH_MODES["$service_name"]="ide"
done

# Colour codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No colour

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------
function log()         { echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
function log_success() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
function log_warn()    { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
function log_error()   { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }

function check_dependencies() {
  log "Checking dependencies..."

  if ! command -v jq >/dev/null 2>&1; then
    log_error "jq not installed.  apt-get install jq"
    exit 1
  fi

  # Check for dialog
  if ! command -v dialog >/dev/null 2>&1; then
    log_error "Dialog not installed. Please install it with: apt-get install dialog"
    exit 1
  fi

  # Check for Docker
  if ! command -v docker >/dev/null 2>&1; then
    log_error "Docker not installed. Please install Docker before proceeding (needed for infrastructure)."
    exit 1
  fi

  # Check for Java
  if ! command -v java >/dev/null 2>&1; then
    log_error "Java not installed. Please install Java 17+ before proceeding."
    exit 1
  fi

  # Check Java version
  java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  # shellcheck disable=SC2071
  if [[ "${java_version}" < "17" ]]; then
    log_warn "Java version ${java_version} detected. Java 17+ is recommended."
  fi

  log_success "All dependencies are installed."
}

function create_directories() {
  mkdir -p "$CONFIG_DIR"
  mkdir -p "$LOGS_DIR"

  # Create logs directory for each service
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    mkdir -p "$LOGS_DIR/$service_name"
  done

  mkdir -p "$LOGS_DIR/postgres"
  mkdir -p "$LOGS_DIR/keycloak"
}

function create_docker_network() {
  # Check if network exists
  if ! docker network inspect "$DOCKER_NETWORK" >/dev/null 2>&1; then
    log "Creating Docker network: $DOCKER_NETWORK"
    docker network create "$DOCKER_NETWORK"
    log_success "Docker network created: $DOCKER_NETWORK"
  else
    log_success "Docker network $DOCKER_NETWORK already exists"
  fi
}

function wait_for_service() {
  local url=$1
  local name=$2
  local max_attempts=${3:-60}     # Default: check for 5 minutes (60 attempts)
  local wait_seconds=${4:-5}      # Default: wait 5 seconds between attempts
  local initial_wait=${5:-0}      # Initial wait before first check
  local acceptable_codes=${6:-"200,202,401,302,301,307,308"}  # Default acceptable HTTP status codes

  if [[ $initial_wait -gt 0 ]]; then
    log "Waiting initial $initial_wait seconds before checking $name..."
    sleep "$initial_wait"
  fi

  log "Waiting for $name to be reachable at $url..."
  log "Max attempts: $max_attempts, wait between attempts: ${wait_seconds}s"
  log "Acceptable HTTP codes: $acceptable_codes"

  for ((i=1; i<=$max_attempts; i++)); do
    # Grab only the HTTP status code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$url")

    # Check if HTTP code is in our acceptable list
    if [[ "$acceptable_codes" == *"$http_code"* ]]; then
      log_success "$name is reachable (HTTP $http_code)!"
      return 0
    fi

    log "Attempt $i/$max_attempts - $name returned HTTP $http_code, retrying in ${wait_seconds}s..."
    sleep "$wait_seconds"
  done

  log_error "$name did not become reachable in time (last HTTP: $http_code)."
  return 1
}

function wait_for_postgres() {
  local host=$1
  local port=$2
  local user=$3
  local password=$4
  local max_attempts=60
  local wait_seconds=2

  log "Waiting for PostgreSQL to be ready at $host:$port..."

  for ((i=1; i<=$max_attempts; i++)); do
    if PGPASSWORD="$password" psql -h "$host" -p "$port" -U "$user" -c "SELECT 1" >/dev/null 2>&1; then
      log_success "PostgreSQL is ready!"
      return 0
    fi
    log "Attempt $i/$max_attempts - PostgreSQL not yet ready, waiting ${wait_seconds}s..."
    sleep "$wait_seconds"
  done

  log_error "PostgreSQL did not become ready in time."
  return 1
}

function view_logs() {
  local options=()

  # Add all services to the options
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    options+=("$service_name" "View logs for $service_name")
  done

  # Add infrastructure services
  options+=(
    "postgres" "View PostgreSQL logs"
    "keycloak" "View Keycloak logs"
    "all" "View all logs (tail -f)"
    "back" "Return to main menu"
  )

  local selection
  selection=$(dialog --clear --backtitle "MDental Platform Launcher" \
    --title "View Service Logs" \
    --menu "Select a service to view logs:" 20 60 14 \
    "${options[@]}" 2>&1 >/dev/tty)

  clear

  case "$selection" in
    "back")
      return
      ;;
    "all")
      # Use multitail or split terminal if available, otherwise just tail everything
      log "Displaying all logs. Press Ctrl+C to exit."
      find "$LOGS_DIR" -name "*.log" -type f -exec tail -f {} \;
      ;;
    *)
      # For IDE-launched services, show a message
      for service_combo in "${SERVICES[@]}"; do
        service_name="${service_combo%%:*}"
        if [[ "$service_name" == "$selection" && "${SERVICE_LAUNCH_MODES[$service_name]}" == "ide" ]]; then
          log_warn "$service_name is running in IDE mode. Check IDE console for logs."
          sleep 3
          return
        fi
      done

      if [[ -d "$LOGS_DIR/$selection" ]]; then
        # If it's a directory, find the latest log file
        local latest_log
        latest_log=$(find "$LOGS_DIR/$selection" -name "*.log" -type f -printf "%T@ %p\n" | sort -n | tail -1 | cut -d' ' -f2-)

        if [[ -n "$latest_log" ]]; then
          log "Viewing logs for $selection. Press Ctrl+C to exit."
          tail -f "$latest_log"
        else
          log_error "No log files found for $selection"
          sleep 2
        fi
      else
        # If it's a single log file
        local log_file="$LOGS_DIR/$selection/$selection.log"
        if [[ -f "$log_file" ]]; then
          log "Viewing logs for $selection. Press Ctrl+C to exit."
          tail -f "$log_file"
        else
          log_error "Log file not found: $log_file"
          sleep 2
        fi
      fi
      ;;
  esac

  # After viewing logs, return to the log menu
  view_logs
}

# -----------------------------------------------------------------------------
# Database Functions
# -----------------------------------------------------------------------------

function start_postgres() {
  local port=$1
  local user=$2
  local password=$3

  log "Starting PostgreSQL container on port $port..."

  # Check if container already exists
  if docker ps -a --format '{{.Names}}' | grep -q "^mdental-postgres$"; then
    log "PostgreSQL container already exists, reusing it."

    # Check if it's running
    if ! docker ps --format '{{.Names}}' | grep -q "^mdental-postgres$"; then
      docker start mdental-postgres
    fi
  else
    # Create the init scripts directory if it doesn't exist
    mkdir -p "$CONFIG_DIR/postgres-init"

    # Create the database init script
    cat > "$CONFIG_DIR/postgres-init/01-create-databases.sh" << EOF
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "\$POSTGRES_USER" --dbname "\$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE keycloak;
    CREATE DATABASE mdental_auth;
    CREATE DATABASE mdental_clinic;

    GRANT ALL PRIVILEGES ON DATABASE keycloak TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE mdental_auth TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE mdental_clinic TO postgres;
EOSQL
EOF

    # Make it executable
    chmod +x "$CONFIG_DIR/postgres-init/01-create-databases.sh"

    # Run PostgreSQL container
    docker run -d --name mdental-postgres \
      --network="$DOCKER_NETWORK" \
      -e POSTGRES_USER="$user" \
      -e POSTGRES_PASSWORD="$password" \
      -p "$port:5432" \
      -v "$CONFIG_DIR/postgres-init:/docker-entrypoint-initdb.d" \
      -v mdental-postgres-data:/var/lib/postgresql/data \
      postgres:14
  fi

  # Wait for PostgreSQL to be ready
  wait_for_postgres "localhost" "$port" "$user" "$password"

  log_success "PostgreSQL container is running!"
}

# -----------------------------------------------------------------------------
# Keycloak Functions
# -----------------------------------------------------------------------------
function start_keycloak() {
  local port=$1
  local admin_user=$2
  local admin_password=$3

  log "Starting Keycloak container on port $port…"

  mkdir -p "$CONFIG_DIR/keycloak"

  # Ensure platform-realm.json is present (copy or create if missing)
  if [[ ! -f "$CONFIG_DIR/keycloak/platform-realm.json" ]]; then
    if [[ -f "$SCRIPT_DIR/infrastructure/keycloak/platform-realm.json" ]]; then
      cp "$SCRIPT_DIR/infrastructure/keycloak/platform-realm.json" "$CONFIG_DIR/keycloak/"
    else
      cat > "$CONFIG_DIR/keycloak/platform-realm.json" <<'EOF'
{
  "realm": "platform",
  "enabled": true,
  "clients": [
    {
      "clientId": "mdental-platform-admin",
      "publicClient": false,
      "secret": "bRFrYYJ9HUyYonPWc9wG2eDQ1OO2KN0l",
      "serviceAccountsEnabled": true,
      "directAccessGrantsEnabled": false,
      "standardFlowEnabled": false
    }
  ],
  "roles": {
    "realm": [
      { "name": "SUPER_ADMIN", "description": "Global super user" },
      { "name": "SUPPORT", "description": "Read-only support staff" }
    ]
  },
  "users": [
    {
      "username": "superadmin",
      "enabled": true,
      "firstName": "Super",
      "lastName": "Admin",
      "email": "super@mdental.org",
      "credentials": [{ "type": "password", "value": "x4SXJuGyIIAmpzcXEFtrrMl5ndycu20b", "temporary": false }],
      "realmRoles": [ "SUPER_ADMIN" ]
    },
    {
      "username": "service-account-mdental-platform-admin",
      "serviceAccountClientId": "mdental-platform-admin",
      "enabled": true,
      "realmRoles": [ "SUPER_ADMIN" ]
    }
  ]
}
EOF
    fi
  fi

  # Create realm template if missing
  if [[ ! -f "$CONFIG_DIR/keycloak/realm-template.json" ]]; then
    if [[ -f "$SCRIPT_DIR/infrastructure/keycloak/realm-template.json" ]]; then
      cp "$SCRIPT_DIR/infrastructure/keycloak/realm-template.json" "$CONFIG_DIR/keycloak/"
    else
      cat > "$CONFIG_DIR/keycloak/realm-template.json" <<'EOF'
{
  "realm": "mdental-__CLINIC_SLUG__",
  "enabled": true,
  "roles": {
    "realm": [
      { "name": "CLINIC_ADMIN", "description": "Clinic administrator with full access" },
      { "name": "DOCTOR", "description": "Doctor with clinical access" },
      { "name": "RECEPTIONIST", "description": "Front desk receptionist" },
      { "name": "PATIENT", "description": "Patient with limited access" }
    ]
  },
  "users": [
    {
      "username": "admin",
      "enabled": true,
      "credentials": [{ "type": "password", "value": "x4SXJuGyIIAmpzcXEFtrrMl5ndycu20b", "temporary": false }],
      "realmRoles": [ "CLINIC_ADMIN" ]
    }
  ]
}
EOF
    fi
  fi

  # Re-use or run container
  if docker ps -a --format '{{.Names}}' | grep -q "^mdental-keycloak$"; then
    log "Keycloak container already exists, re-using."
    if ! docker ps --format '{{.Names}}' | grep -q "^mdental-keycloak$"; then
      docker start mdental-keycloak
    fi
  else
    docker run -d --name mdental-keycloak \
      --network="$DOCKER_NETWORK" \
      -e KC_DB=postgres \
      -e KC_DB_URL="jdbc:postgresql://mdental-postgres:5432/keycloak" \
      -e KC_DB_USERNAME="$POSTGRES_USER" \
      -e KC_DB_PASSWORD="$POSTGRES_PASSWORD" \
      -e KEYCLOAK_ADMIN="$admin_user" \
      -e KEYCLOAK_ADMIN_PASSWORD="$admin_password" \
      -e KC_HEALTH_ENABLED=true \
      -e KC_SPI_IMPORT_IMPORT_STRATEGY=IGNORE_EXISTING \
      -v "$CONFIG_DIR/keycloak:/opt/keycloak/data/import" \
      -p "$port:8080" \
      quay.io/keycloak/keycloak:24.0.1 \
      start-dev --import-realm
  fi

  # Keycloak 24 exposes readiness at /health/ready
  if ! wait_for_service "http://localhost:$port/health/ready" "Keycloak" 60 5 10 "200"; then
    log_warn "Keycloak did not respond in time; continuing anyway (it may still be booting)."
  fi
  log_success "Keycloak container is running!"
}

# ----------------------------------------------------------
# Obtain SUPER_ADMIN token once Keycloak is up
# ----------------------------------------------------------
function get_super_admin_token() {
  for i in {1..10}; do          # ≤20 s total
    _get_super_admin_token && return 0
    sleep 2
  done
  return 1
}

function _get_super_admin_token() {
  [[ -n "$SUPER_ADMIN_TOKEN" ]] && return 0

  log "Requesting SUPER_ADMIN token via client_credentials…"
  token_json=$(curl -s \
      -X POST "${KEYCLOAK_URL}/realms/platform/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=client_credentials" \
      -d "client_id=${PLATFORM_REALM_CLIENT_ID}" \
      -d "client_secret=${PLATFORM_REALM_CLIENT_SECRET}" \
      -d "scope=roles" \
      )

  SUPER_ADMIN_TOKEN=$(echo "$token_json" | jq -r .access_token)
  if [[ -z "$SUPER_ADMIN_TOKEN" || "$SUPER_ADMIN_TOKEN" == "null" ]]; then
    log_error "Failed to obtain token: $token_json"
    return 1
  fi

  log_success "🔑  Service-account token acquired."
}

# create_new_realm re-implemented to use auth-core directly
function create_new_realm() {
  # --- 1) prompt & validate slug ---
  local clinic_slug
  clinic_slug=$(dialog --clear --backtitle "MDental Platform Launcher" \
                --title "Create New Clinic Realm" \
                --inputbox "Clinic slug (lowercase, numbers, hyphens):" 8 60 \
                2>&1 >/dev/tty) || return 1

  if [[ ! "$clinic_slug" =~ ^[a-z0-9-]+$ ]]; then
    dialog --backtitle "MDental Platform Launcher" \
           --title "Invalid slug" \
           --msgbox "Allowed chars: a-z, 0-9, and '-'" 7 50
    return 1
  fi

  clear
  log "🛠  Creating realm mdental-${clinic_slug}…"

  # --- 2) obtain SUPER_ADMIN token (platform realm) ---
  if ! get_super_admin_token; then
    dialog --title "Token Error" \
      --msgbox "Could not fetch SUPER_ADMIN token.\nCheck logs under $LOGS_DIR/auth-core for details." 8 60
    return 1
  fi

  # --- 3) call auth-core, dump full response to logfile ---
  local logfile http_code
  mkdir -p "$LOGS_DIR/auth-core"
  logfile="$LOGS_DIR/auth-core/realm-create-$(date +%Y%m%d-%H%M%S).log"

  # capture full request+response (inc. stderr) for inspection
  # use -v to include any OIDC error details on stderr
  curl -s -i -v -X POST \
    -H "Authorization: Bearer ${SUPER_ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    --data "{\"realm\":\"mdental-${clinic_slug}\",\"clinicSlug\":\"${clinic_slug}\"}" \
    "${AUTH_CORE_URL}/realms" \
    > "$logfile" 2>&1

  # extract the first HTTP status line we see, fall back to 000
  http_code=$(grep -m1 -Eo 'HTTP/1\.[01] [0-9]{3}' "$logfile" | awk '{print $2}' || echo "000")
  log "→ Response HTTP $http_code"
  log "→ Logged full response to $logfile"

  # --- 4) success / conflict / error UI ---
  case "$http_code" in
    200)
      dialog --title "Success" --msgbox "✅  Realm mdental-${clinic_slug} created." 7 50
      ;;
    409)
      dialog --title "Already Exists" --msgbox "ℹ️  Realm mdental-${clinic_slug} already exists." 7 50
      ;;
    *)
      dialog --title "Error (HTTP $http_code)" \
             --textbox "$logfile" 20 70
      ;;
  esac
}

# -----------------------------------------------------------------------------
# Service-management helpers (wait_for_ide_service, start_all_ide_services, etc.)
# -----------------------------------------------------------------------------

function wait_for_ide_service() {
  local service_name=$1
  local service_port=$2

  log "Checking if $service_name is available on port $service_port..."

  # Define URLs to check
  local urls=(
    "http://localhost:$service_port/actuator/health"
    "http://localhost:$service_port"
  )

  # Try each URL
  for url in "${urls[@]}"; do
    # First quick check if it's already running
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "$url")
    if [[ "$http_code" == "200" || "$http_code" == "401" || "$http_code" == "302" || "$http_code" == "301" ]]; then
      log_success "$service_name is already running at $url (HTTP $http_code)"
      return 0
    fi
  done

  # If we get here, service isn't running
  log_warn "$service_name doesn't appear to be running."
  log_warn "Please start $service_name in your IDE manually."

  # Prompt user to continue
  read -p "Press ENTER when $service_name is started in your IDE (or enter 'skip' to continue anyway): " response

  if [[ "$response" == "skip" ]]; then
    log_warn "Skipping wait for $service_name"
    return 0
  fi

  # Wait for the service with extended timeouts
  for url in "${urls[@]}"; do
    if wait_for_service "$url" "$service_name" 60 5 0; then
      return 0
    fi
  done

  log_error "Could not verify $service_name is running after waiting. Continuing anyway, but service may not be available."
  return 0  # Continue even if the service isn't ready
}

function start_all_ide_services() {
  log "Checking all IDE-run services..."

  # Check services in order
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    service_port="${service_combo#*:}"

    wait_for_ide_service "$service_name" "$service_port"
  done

  log_success "All IDE services checked."
}

# -----------------------------------------------------------------------------
# Configuration UI
# -----------------------------------------------------------------------------

function show_configuration_menu() {
  local selection

  while true; do
    selection=$(dialog --clear --backtitle "MDental Platform Launcher" \
      --title "Configuration Menu" \
      --menu "Configure deployment settings:" 16 60 7 \
      "postgres" "Configure PostgreSQL (current: $POSTGRES_PORT)" \
      "keycloak" "Configure Keycloak (current: $KEYCLOAK_PORT)" \
      "services" "Select services to deploy" \
      "save" "Save configuration" \
      "back" "Return to main menu" \
      2>&1 >/dev/tty)

    # Check if user cancelled
    if [[ $? -ne 0 ]]; then
      return
    fi

    case "$selection" in
      "postgres")
        configure_postgres
        ;;
      "keycloak")
        configure_keycloak
        ;;
      "services")
        select_services
        ;;
      "save")
        save_configuration
        ;;
      "back")
        return
        ;;
    esac
  done
}

function configure_postgres() {
  local result

  result=$(dialog --clear --backtitle "MDental Platform Launcher" \
    --title "PostgreSQL Configuration" \
    --form "Configure PostgreSQL connection:" 15 60 3 \
    "Port:" 1 1 "$POSTGRES_PORT" 1 20 10 0 \
    "Username:" 2 1 "$POSTGRES_USER" 2 20 20 0 \
    "Password:" 3 1 "$POSTGRES_PASSWORD" 3 20 20 0 \
    2>&1 >/dev/tty)

  # Check if user cancelled
  if [[ $? -ne 0 ]]; then
    return
  fi

  # Parse result
  IFS=$'\n' read -r -d '' -a values < <(echo -n "$result")

  POSTGRES_PORT="${values[0]}"
  POSTGRES_USER="${values[1]}"
  POSTGRES_PASSWORD="${values[2]}"
}

function configure_keycloak() {
  local result

  result=$(dialog --clear --backtitle "MDental Platform Launcher" \
    --title "Keycloak Configuration" \
    --form "Configure Keycloak connection:" 15 60 3 \
    "Port:" 1 1 "$KEYCLOAK_PORT" 1 20 10 0 \
    "Admin User:" 2 1 "$KEYCLOAK_ADMIN" 2 20 20 0 \
    "Admin Password:" 3 1 "$KEYCLOAK_ADMIN_PASSWORD" 3 20 20 0 \
    2>&1 >/dev/tty)

  # Check if user cancelled
  if [[ $? -ne 0 ]]; then
    return
  fi

  # Parse result
  IFS=$'\n' read -r -d '' -a values < <(echo -n "$result")

  KEYCLOAK_PORT="${values[0]}"
  KEYCLOAK_ADMIN="${values[1]}"
  KEYCLOAK_ADMIN_PASSWORD="${values[2]}"
}

function select_services() {
  local options=()
  local selected_services=()

  # Build options array for dialog
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    service_port="${service_combo#*:}"

    # Default to ON for all services
    options+=("$service_combo" "$service_name (port: $service_port)" "ON")
  done

  # Show dialog
  local result
  result=$(dialog --clear --backtitle "MDental Platform Launcher" \
    --title "Select Services" \
    --checklist "Choose services to deploy:" 15 60 10 \
    "${options[@]}" \
    2>&1 >/dev/tty)

  # Check if user cancelled
  if [[ $? -ne 0 ]]; then
    return
  fi

  # Parse result
  for selection in $result; do
    selected_services+=("$(echo "$selection" | tr -d '"')")
  done

  # Update SERVICES array (if empty, keep all services)
  if [[ ${#selected_services[@]} -gt 0 ]]; then
    SERVICES=("${selected_services[@]}")
  fi
}

function save_configuration() {
  cat > "$CONFIG_DIR/launch.conf" <<EOF
# MDental Platform Launcher Configuration
# Generated on $(date)

# PostgreSQL
POSTGRES_PORT=$POSTGRES_PORT
POSTGRES_USER=$POSTGRES_USER
POSTGRES_PASSWORD=$POSTGRES_PASSWORD

# Keycloak
KEYCLOAK_PORT=$KEYCLOAK_PORT
KEYCLOAK_ADMIN=$KEYCLOAK_ADMIN
KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD

# Super-admin / Auth-core
SUPER_ADMIN_USER=$SUPER_ADMIN_USER
SUPER_ADMIN_PASS=$SUPER_ADMIN_PASS
AUTH_CORE_URL=$AUTH_CORE_URL

# Services
EOF

  for service_combo in "${SERVICES[@]}"; do
    echo "SERVICE=$service_combo" >> "$CONFIG_DIR/launch.conf"
  done

  dialog --clear --backtitle "MDental Platform Launcher" \
    --title "Configuration Saved" \
    --msgbox "Configuration saved to $CONFIG_DIR/launch.conf" 8 60
}

function load_configuration() {
  if [[ -f "$CONFIG_DIR/launch.conf" ]]; then
    log "Loading configuration from $CONFIG_DIR/launch.conf"
    # shellcheck source=/dev/null
    source "$CONFIG_DIR/launch.conf"

    # Rebuild SERVICES if config contains SERVICE= lines
    if grep -q "^SERVICE=" "$CONFIG_DIR/launch.conf"; then
      SERVICES=()
      while IFS= read -r line; do
        [[ "$line" =~ ^SERVICE=(.*) ]] && SERVICES+=("${BASH_REMATCH[1]}")
      done < "$CONFIG_DIR/launch.conf"
    fi

    log_success "Configuration loaded."
  else
    log_warn "No configuration file found – using defaults."
  fi
}

# -----------------------------------------------------------------------------
# Main Menu & Workflow
# -----------------------------------------------------------------------------
function main() {
  clear
  cat <<'BANNER'
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                                                                              ┃
┃   ███╗   ███╗██████╗ ███████╗███╗   ██╗████████╗ █████╗ ██╗                  ┃
┃   ████╗ ████║██╔══██╗██╔════╝████╗  ██║╚══██╔══╝██╔══██╗██║                  ┃
┃   ██╔████╔██║██║  ██║█████╗  ██╔██╗ ██║   ██║   ███████║██║                  ┃
┃   ██║╚██╔╝██║██║  ██║██╔══╝  ██║╚██╗██║   ██║   ██╔══██║██║                  ┃
┃   ██║ ╚═╝ ██║██████╔╝███████╗██║ ╚████║   ██║   ██║  ██║███████╗             ┃
┃   ╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝   ╚═╝  ╚═╝╚══════╝             ┃
┃                                                                              ┃
┃              MDental Platform Launcher – v1.1.0                              ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
BANNER

  sleep 1
  check_dependencies
  create_directories
  load_configuration

  local selection
  while true; do
    selection=$(dialog --clear --backtitle "MDental Platform Launcher" \
      --title "Main Menu" \
      --menu "Choose an option:" 15 60 8 \
      "start"      "Start all services" \
      "configure"  "Configure deployment" \
      "health"     "Check services health" \
      "logs"       "View logs" \
      "realm"      "Create New Clinic Realm" \
      "status"     "Service status overview" \
      "stop"       "Stop infrastructure services" \
      "exit"       "Exit launcher" \
      2>&1 >/dev/tty)

    [[ $? -ne 0 ]] && selection="exit"
    clear

    case "$selection" in
      start)      start_all_services   ;;
      configure)  show_configuration_menu ;;
      health)     check_services_health ;;
      logs)       view_logs            ;;
      realm)      create_new_realm     ;;
      status)     check_services_status ;;
      stop)       stop_infrastructure  ;;
      exit)       log "Exiting launcher…"; exit 0 ;;
    esac
  done
}

function start_all_services() {
  log "Starting all services…"
  create_docker_network
  start_postgres "$POSTGRES_PORT" "$POSTGRES_USER" "$POSTGRES_PASSWORD"
  start_keycloak "$KEYCLOAK_PORT" "$KEYCLOAK_ADMIN" "$KEYCLOAK_ADMIN_PASSWORD"

  # Import platform realm automatically handled by --import-realm flag
  # so no separate import_keycloak_realm call needed.

  # Obtain token once Keycloak is alive
  get_super_admin_token || {
    log_error "Super-admin token retrieval failed – realm creation feature will be unavailable."
  }

  start_all_ide_services
  check_services_status

  log_success "Infrastructure ready. IDE services checked."

  local summary_msg="Service summary:\n\n"
  summary_msg+="Infrastructure:\n"
  summary_msg+="• PostgreSQL: Docker on port $POSTGRES_PORT\n"
  summary_msg+="• Keycloak:  Docker on port $KEYCLOAK_PORT\n\n"
  summary_msg+="IDE-run micro-services:\n"

  for svc in "${SERVICES[@]}"; do
    name="${svc%%:*}"; port="${svc#*:}"
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" | grep -q -E "200|401|302" ||
       curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port"              | grep -q -E "200|401|302"; then
      summary_msg+="• $name (:$port) – ✅\n"
    else
      summary_msg+="• $name (:$port) – ❌\n"
    fi
  done

  summary_msg+="\nAccess Points:\n"
  summary_msg+="• Gateway   → http://localhost:8080\n"
  summary_msg+="• Keycloak  → http://localhost:$KEYCLOAK_PORT/admin\n"
  summary_msg+="• Eureka UI → http://localhost:8761\n"

  dialog --clear --backtitle "MDental Platform Launcher" \
    --title "Infrastructure Ready" \
    --msgbox "$summary_msg" 25 65
}

function check_services_health() {
  log "Checking health of all services..."

  local all_healthy=true
  local health_report="Service Health Report:\n\n"

  # Check infrastructure
  for service in "PostgreSQL:$POSTGRES_PORT" "Keycloak:$KEYCLOAK_PORT"; do
    IFS=':' read -r service_name service_port <<< "$service"

    if [[ "$service_name" == "PostgreSQL" ]]; then
      if PGPASSWORD="$POSTGRES_PASSWORD" psql -h "localhost" -p "$service_port" -U "$POSTGRES_USER" -c "SELECT 1" >/dev/null 2>&1; then
        health_report+="✅ $service_name is healthy\n"
      else
        health_report+="❌ $service_name is NOT healthy\n"
        all_healthy=false
      fi
    elif [[ "$service_name" == "Keycloak" ]]; then
      if curl -s -f "http://localhost:$service_port/health" >/dev/null 2>&1; then
        health_report+="✅ $service_name is healthy\n"
      else
        health_report+="❌ $service_name is NOT healthy\n"
        all_healthy=false
      fi
    fi
  done

  health_report+="\n"

  # Check IDE services
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    service_port="${service_combo#*:}"

    # Try both health endpoint and root URL
    local health_status=false
    local http_code="000"

    # Try health endpoint first
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:$service_port/actuator/health")
    if [[ "$http_code" == "200" || "$http_code" == "401" || "$http_code" == "302" ]]; then
      health_status=true
    else
      # Try root URL as fallback
      http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:$service_port")
      if [[ "$http_code" == "200" || "$http_code" == "401" || "$http_code" == "302" ]]; then
        health_status=true
      fi
    fi

    if [[ "$health_status" == "true" ]]; then
      health_report+="✅ $service_name is healthy (HTTP $http_code)\n"
    else
      health_report+="❌ $service_name is NOT healthy (HTTP $http_code)\n"
      all_healthy=false
    fi
  done

  # Display health report
  if [[ "$all_healthy" == "true" ]]; then
    health_report+="\n✅ All services are healthy!"
    dialog --clear --backtitle "MDental Platform Launcher" \
      --title "Health Check: PASSED" \
      --msgbox "$health_report" 20 60
  else
    health_report+="\n❌ Some services are not healthy. Check logs for details."
    dialog --clear --backtitle "MDental Platform Launcher" \
      --title "Health Check: ISSUES FOUND" \
      --msgbox "$health_report" 20 60
  fi
}

function check_services_status() {
  log "Checking services status..."

  # Check PostgreSQL
  if PGPASSWORD="$POSTGRES_PASSWORD" psql -h "localhost" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -c "SELECT 1" >/dev/null 2>&1; then
    log_success "PostgreSQL is running on port $POSTGRES_PORT"
  else
    log_error "PostgreSQL is not running!"
  fi

  # Check Keycloak
  if curl -s -f "http://localhost:$KEYCLOAK_PORT/health" >/dev/null 2>&1; then
    log_success "Keycloak is running on port $KEYCLOAK_PORT"
  else
    log_error "Keycloak is not running!"
  fi

  # Check all services
  for service_combo in "${SERVICES[@]}"; do
    service_name="${service_combo%%:*}"
    service_port="${service_combo#*:}"

    if [[ "$service_name" == "discovery-server" ]]; then
      # Check Eureka - try actuator first, then fall back to root URL
      if curl -s -f "http://localhost:$service_port/actuator/health" >/dev/null 2>&1 ||
         curl -s -f "http://localhost:$service_port" >/dev/null 2>&1; then
        log_success "Eureka discovery server is running on port $service_port (IDE mode)"
      else
        log_error "Eureka discovery server is not running!"
      fi
    else
      # Check other services via their health endpoint
      local status_code
      status_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:$service_port/actuator/health")

      if [[ "$status_code" == "200" || "$status_code" == "401" || "$status_code" == "302" ]]; then
        log_success "$service_name is running on port $service_port (IDE mode)"
      else
        # Try root URL as fallback
        status_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "http://localhost:$service_port")
        if [[ "$status_code" == "200" || "$status_code" == "401" || "$status_code" == "302" ]]; then
          log_success "$service_name is running on port $service_port (IDE mode)"
        else
          log_error "$service_name is not running! (HTTP $status_code)"
        fi
      fi
    fi
  done

  log "Services status check completed."

  # Press any key to continue
  read -n 1 -s -r -p "Press any key to continue..."
  echo
}

function stop_infrastructure() {
  log "Stopping infrastructure services (PostgreSQL, Keycloak)..."

  # Stop infrastructure containers
  for container in "mdental-keycloak" "mdental-postgres"; do
    if docker ps --format '{{.Names}}' | grep -q "^$container$"; then
      log "Stopping $container..."
      docker stop "$container"
    else
      log_warn "$container is not running."
    fi
  done

  log_success "All infrastructure services stopped."
  log_warn "Note: Services running in IDE mode need to be stopped manually in your IDE."

  # Press any key to continue
  read -n 1 -s -r -p "Press any key to continue..."
  echo
}

# -----------------------------------------------------------------------------
# Guard: ensure not sourced, then execute main
# -----------------------------------------------------------------------------
if [[ "${BASH_SOURCE[0]}" != "${0}" ]]; then
  log_error "This script must be executed, not sourced."
  return 1
fi

main "$@"
