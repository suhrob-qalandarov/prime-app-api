#!/bin/bash

set -e

DEPLOY_DIR="/home/${USER}/prime_app"
ACTIVE_ENV_FILE="${DEPLOY_DIR}/.active_env"
BLUE_PORT=8080
GREEN_PORT=8081

# Determine current active environment
if [ -f "$ACTIVE_ENV_FILE" ]; then
    CURRENT_ACTIVE=$(cat "$ACTIVE_ENV_FILE")
else
    CURRENT_ACTIVE="blue"
    echo "blue" > "$ACTIVE_ENV_FILE"
fi

# Determine target environment (opposite of current)
if [ "$CURRENT_ACTIVE" == "blue" ]; then
    TARGET_ENV="green"
    TARGET_PORT=$GREEN_PORT
    TARGET_COMPOSE="docker-compose.green.yml"
    TARGET_SERVICE="app_green"
    TARGET_CONTAINER="prime_app_green"
    CURRENT_SERVICE="app_blue"
    CURRENT_CONTAINER="prime_app_blue"
else
    TARGET_ENV="blue"
    TARGET_PORT=$BLUE_PORT
    TARGET_COMPOSE="docker-compose.blue.yml"
    TARGET_SERVICE="app_blue"
    TARGET_CONTAINER="prime_app_blue"
    CURRENT_SERVICE="app_green"
    CURRENT_CONTAINER="prime_app_green"
fi

echo "Current active environment: $CURRENT_ACTIVE"
echo "Deploying to: $TARGET_ENV"

cd "$DEPLOY_DIR"

# Ensure network exists
echo "Ensuring app-network exists..."
docker network create app-network 2>/dev/null || echo "Network app-network already exists or created"

# Stop target environment container if running (only the app container, not db/nginx)
echo "Stopping $TARGET_ENV environment container..."
docker stop "$TARGET_CONTAINER" 2>/dev/null || echo "Container $TARGET_CONTAINER not running"
docker rm "$TARGET_CONTAINER" 2>/dev/null || echo "Container $TARGET_CONTAINER not found"

# Ensure db and nginx are running
echo "Ensuring db and nginx are running..."
docker compose -f docker-compose.yml up -d db nginx

# Build and start target environment (only the app service)
echo "Building and starting $TARGET_ENV environment..."
docker compose -f docker-compose.yml -f "$TARGET_COMPOSE" build --no-cache "$TARGET_SERVICE"
docker compose -f docker-compose.yml -f "$TARGET_COMPOSE" up -d "$TARGET_SERVICE"

# Wait for health check
echo "Waiting for $TARGET_ENV to be healthy..."
MAX_WAIT=120
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
    echo "ERROR: $TARGET_ENV failed to become healthy within $MAX_WAIT seconds"
    echo "Rolling back to $CURRENT_ACTIVE..."
    docker stop "$TARGET_CONTAINER" 2>/dev/null || true
    docker rm "$TARGET_CONTAINER" 2>/dev/null || true
    exit 1
fi

# Update nginx configuration to point to target environment
echo "Switching nginx to $TARGET_ENV..."
if [ "$TARGET_ENV" == "blue" ]; then
    sed -i 's/server prime_app_green:8081;/server prime_app_blue:8080;/' nginx/conf.d/app.conf
    sed -i 's/# server prime_app_blue:8080 backup;/# server prime_app_green:8081 backup;/' nginx/conf.d/app.conf
else
    sed -i 's/server prime_app_blue:8080;/server prime_app_green:8081;/' nginx/conf.d/app.conf
    sed -i 's/# server prime_app_green:8081 backup;/# server prime_app_blue:8080 backup;/' nginx/conf.d/app.conf
fi

# Reload nginx (ensure it's running first)
if ! docker ps | grep -q prime_nginx; then
    echo "Starting nginx..."
    docker compose -f docker-compose.yml up -d nginx
    sleep 2
fi
docker exec prime_nginx nginx -s reload

# Update active environment file
echo "$TARGET_ENV" > "$ACTIVE_ENV_FILE"

# Stop old environment container (optional - can keep for quick rollback)
echo "Stopping old $CURRENT_ACTIVE environment container..."
docker stop "$CURRENT_CONTAINER" 2>/dev/null || echo "Container $CURRENT_CONTAINER not running"
docker rm "$CURRENT_CONTAINER" 2>/dev/null || echo "Container $CURRENT_CONTAINER not found"

echo "Deployment to $TARGET_ENV completed successfully!"
echo "Active environment: $TARGET_ENV"


