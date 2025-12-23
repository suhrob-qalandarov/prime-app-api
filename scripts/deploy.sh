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

# Ensure db is running
echo "Ensuring db is running..."
docker compose -f docker-compose.yml up -d db

# Build and start target environment (only the app service)
echo "Building and starting $TARGET_ENV environment..."
docker compose -f docker-compose.yml -f "$TARGET_COMPOSE" build --no-cache "$TARGET_SERVICE"
docker compose -f docker-compose.yml -f "$TARGET_COMPOSE" up -d "$TARGET_SERVICE"

# Wait for health check
echo "Waiting for $TARGET_ENV to be healthy..."
MAX_WAIT=40
WAIT_TIME=0
HEALTHY=false

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    # Check Docker healthcheck status
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$TARGET_CONTAINER" 2>/dev/null || echo "starting")
    
    if [ "$HEALTH_STATUS" == "healthy" ]; then
        echo "$TARGET_ENV is healthy!"
        HEALTHY=true
        break
    fi
    
    # Also try direct curl check as fallback
    if docker exec "$TARGET_CONTAINER" curl -f http://localhost:$TARGET_PORT/actuator/health > /dev/null 2>&1; then
        echo "$TARGET_ENV is healthy (via curl)!"
        HEALTHY=true
        break
    fi
    
    echo "Waiting for $TARGET_ENV to be healthy... ($WAIT_TIME/$MAX_WAIT seconds) [Status: $HEALTH_STATUS]"
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))
done

if [ "$HEALTHY" != "true" ]; then
    echo "ERROR: $TARGET_ENV failed to become healthy within $MAX_WAIT seconds"
    echo "Container logs:"
    docker logs --tail 50 "$TARGET_CONTAINER" 2>/dev/null || true
    echo "Rolling back to $CURRENT_ACTIVE..."
    docker stop "$TARGET_CONTAINER" 2>/dev/null || true
    docker rm "$TARGET_CONTAINER" 2>/dev/null || true
    exit 1
fi

# Update nginx configuration to point to target environment
echo "Updating nginx configuration to $TARGET_ENV environment (port $TARGET_PORT)..."
NGINX_CONFIG="/etc/nginx/conf.d/app.conf"

if [ -f "$NGINX_CONFIG" ]; then
    # Update upstream server port in nginx config
    # Replace server localhost:8080 or server localhost:8081 with target port
    sudo sed -i "s/server localhost:[0-9]*;/server localhost:$TARGET_PORT;/" "$NGINX_CONFIG"
    
    # Also update if using container names (for Docker network)
    if [ "$TARGET_ENV" == "blue" ]; then
        sudo sed -i "s/server prime_app_green:[0-9]*;/server prime_app_blue:8080;/" "$NGINX_CONFIG"
    else
        sudo sed -i "s/server prime_app_blue:[0-9]*;/server prime_app_green:8081;/" "$NGINX_CONFIG"
    fi
    
    # Test nginx config
    if sudo nginx -t > /dev/null 2>&1; then
        # Reload nginx
        sudo systemctl reload nginx
        echo "Nginx configuration updated and reloaded successfully"
        echo "Nginx now points to $TARGET_ENV environment on port $TARGET_PORT"
    else
        echo "ERROR: Nginx configuration test failed. Please check manually."
        sudo nginx -t
        exit 1
    fi
else
    echo "WARNING: Nginx config file not found at $NGINX_CONFIG"
    echo "Please manually update nginx to point to port $TARGET_PORT"
fi

# Print active environment info
echo "=========================================="
echo "Active environment: $TARGET_ENV"
echo "Application running on port: $TARGET_PORT"
echo "Container: $TARGET_CONTAINER"
echo "=========================================="

# Update active environment file
echo "$TARGET_ENV" > "$ACTIVE_ENV_FILE"

# Stop old environment container (optional - can keep for quick rollback)
echo "Stopping old $CURRENT_ACTIVE environment container..."
docker stop "$CURRENT_CONTAINER" 2>/dev/null || echo "Container $CURRENT_CONTAINER not running"
docker rm "$CURRENT_CONTAINER" 2>/dev/null || echo "Container $CURRENT_CONTAINER not found"

echo "Deployment to $TARGET_ENV completed successfully!"
echo "Active environment: $TARGET_ENV"


