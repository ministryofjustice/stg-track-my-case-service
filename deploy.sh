#!/bin/bash
set -euo pipefail

# Define the service name
SERVICE_NAME="stg-track-my-case-service"

# Check if the Docker container is running
if [ "$(docker ps -q -f name=${SERVICE_NAME})" ]; then
    echo "Stopping the running Docker container..."
    docker compose down
fi

echo "Building application JAR for Docker..."
./gradlew bootJar -Dorg.gradle.daemon=false || {
    echo "ERROR: Gradle build failed."
    exit 1
}

if [ ! -f build/libs/stg-track-my-case-service.jar ]; then
    echo "ERROR: build/libs/stg-track-my-case-service.jar was not created. Run: ./gradlew bootJar"
    exit 1
fi

# Rebuild the Docker image
echo "Rebuilding the Docker image..."
docker compose build

# Deploy the service
echo "Starting the Docker container..."
docker compose up -d

echo "Deployment completed successfully."
