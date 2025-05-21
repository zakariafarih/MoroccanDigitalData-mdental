#!/bin/bash

# Ensure we're in the project root
cd "$(dirname "$0")"

# Create logs directory if missing
mkdir -p logs

echo "Installing shared libraries..."
mvn install -pl shared-libs/commons,shared-libs/security-commons -am -DskipTests

echo ""
echo "Starting all core services..."

run_service() {
  local service=$1
  local log_file="logs/${service}.log"
  echo " → core-services/${service}  (logging to $log_file)"
  (cd "core-services/${service}" && mvn spring-boot:run > "../../$log_file" 2>&1 &)
}

run_service auth-core
run_service clinic-core
run_service gateway-core
run_service patient-core

echo ""
echo "All services launching in background."
echo "You can monitor logs with:"
echo "  tail -f logs/*.log"
