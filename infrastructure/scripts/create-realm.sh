#!/usr/bin/env bash
set -euo pipefail

# --- Determine the script’s own directory, so paths resolve correctly ---
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
KEYCLOAK_DIR="${SCRIPT_DIR}/../keycloak"

# --- Input validation ---
if [ $# -ne 1 ]; then
  echo "Error: Clinic name is required."
  echo "Usage: $0 <clinic_name>"
  exit 1
fi
CLINIC_NAME="$1"

# --- Configuration ---
KEYCLOAK_URL="http://localhost:9080"
KEYCLOAK_ADMIN="admin"
KEYCLOAK_ADMIN_PASSWORD="admin"
REALM_TEMPLATE="${KEYCLOAK_DIR}/realm-template.json"
OUTPUT_JSON="/tmp/realm-${CLINIC_NAME}.json"

# --- Ensure the template exists ---
if [ ! -f "$REALM_TEMPLATE" ]; then
  echo "Error: Realm template file not found: $REALM_TEMPLATE"
  exit 1
fi

echo "Preparing Keycloak realm configuration for clinic: $CLINIC_NAME"

# --- Populate the template ---
sed "s/__CLINIC_NAME__/${CLINIC_NAME}/g" "$REALM_TEMPLATE" > "$OUTPUT_JSON"

# --- Authenticate to Keycloak as admin ---
echo "Authenticating with Keycloak admin..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${KEYCLOAK_ADMIN}" \
  -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | grep -o '"access_token":"[^"]*' \
  | sed 's/"access_token":"//')

if [ -z "$TOKEN" ]; then
  echo "Error: Failed to obtain admin token from Keycloak."
  exit 1
fi

# --- Create the new realm ---
echo "Creating realm mdental-${CLINIC_NAME}..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  --data @"$OUTPUT_JSON")

if [[ "$HTTP_STATUS" -eq 201 || "$HTTP_STATUS" -eq 204 ]]; then
  echo "✅ Success! Realm mdental-${CLINIC_NAME} created."
  echo "👉 Realm URL: ${KEYCLOAK_URL}/realms/mdental-${CLINIC_NAME}"
  echo
  echo "Next: Register the clinic in your DB:"
  echo "  curl -X POST http://localhost:8082/internal/dev/clinics \\"
  echo "    -H 'Content-Type: application/json' \\"
  echo "    -d '{\"name\":\"${CLINIC_NAME}\",\"realm\":\"mdental-${CLINIC_NAME}\"}'"
else
  echo "❌ Error: Failed to create realm (HTTP status: $HTTP_STATUS)."
  exit 1
fi

# --- Cleanup ---
rm -f "$OUTPUT_JSON"
echo "Done."
