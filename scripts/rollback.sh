#!/bin/bash

set -e

DEPLOY_DIR="/home/${USER}/prime_app"
ACTIVE_ENV_FILE="${DEPLOY_DIR}/.active_env"

if [ ! -f "$ACTIVE_ENV_FILE" ]; then
    echo "No active environment file found. Cannot rollback."
    exit 1
fi

CURRENT_ACTIVE=$(cat "$ACTIVE_ENV_FILE")

if [ "$CURRENT_ACTIVE" == "blue" ]; then
    TARGET_ENV="green"
    TARGET_COMPOSE="docker-compose.green.yml"
    TARGET_CONTAINER="prime_app_green"
    TARGET_PORT=8081
else
    TARGET_ENV="blue"
    TARGET_COMPOSE="docker-compose.blue.yml"
    TARGET_CONTAINER="prime_app_blue"
    TARGET_PORT=8080
fi

echo "Rolling back from $CURRENT_ACTIVE to $TARGET_ENV..."

cd "$DEPLOY_DIR"

# Start target environment
docker compose -f docker-compose.yml -f "$TARGET_COMPOSE" up -d

# Wait for health check
echo "Waiting for $TARGET_ENV to be healthy..."
MAX_WAIT=60
WAIT_TIME=0
while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if docker exec "$TARGET_CONTAINER" curl -f http://localhost:$TARGET_PORT/actuator/health > /dev/null 2>&1; then
        echo "$TARGET_ENV is healthy!"
        break
    fi
    echo "Waiting for $TARGET_ENV to be healthy... ($WAIT_TIME/$MAX_WAIT seconds)"
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))
done

if [ $WAIT_TIME -ge $MAX_WAIT ]; then
    echo "ERROR: $TARGET_ENV failed to become healthy. Rollback failed."
    exit 1
fi

# Update nginx
if [ "$TARGET_ENV" == "blue" ]; then
    sed -i 's/server prime_app_green:8081;/server prime_app_blue:8080;/' nginx/conf.d/app.conf
    sed -i 's/# server prime_app_blue:8080 backup;/# server prime_app_green:8081 backup;/' nginx/conf.d/app.conf
else
    sed -i 's/server prime_app_blue:8080;/server prime_app_green:8081;/' nginx/conf.d/app.conf
    sed -i 's/# server prime_app_green:8081 backup;/# server prime_app_blue:8080 backup;/' nginx/conf.d/app.conf
fi

docker exec prime_nginx nginx -s reload
echo "$TARGET_ENV" > "$ACTIVE_ENV_FILE"

echo "Rollback completed to $TARGET_ENV"


