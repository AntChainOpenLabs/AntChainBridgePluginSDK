#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/../../config.properties"
CHAIN_TYPE="hyperchain"
source "$SCRIPT_DIR"/../utils.sh


get_config_values "$CONFIG_FILE" "$CHAIN_TYPE" version


# Get all container IDs created using hyperchaincn/solo:v2.0.0
container_ids=$(docker ps -a --filter "ancestor=hyperchaincn/solo:v$version" --format "{{.ID}}")

# Check if any containers are found
if [ -z "$container_ids" ]; then
  log "INFO" "No containers found for hyperchaincn/solo:v$version."
  exit 0
fi

# Stop the containers
log "INFO" "Stopping all containers created using hyperchaincn/solo:v$version..."
docker stop $container_ids

# Remove the containers
log "INFO" "Removing all containers created using hyperchaincn/solo:v$version..."
docker rm $container_ids

log "INFO" "All related containers have been stopped and removed."