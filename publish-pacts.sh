#!/bin/bash

# Load static env vars from .env
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

# Dynamically export current Git commit and branch
export GIT_COMMIT=$(git rev-parse HEAD)

if [ -n "$GIT_BRANCH" ]; then
  BRANCH_NAME="$GIT_BRANCH"
else
  BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)
fi

export GIT_BRANCH="$BRANCH_NAME"

# Run Gradle tasks: tests and publish pacts
./gradlew test pactContractTest pactPublish


# verifying existence of build/pacts directory
PACT_DIR="build/pacts"
if [ ! -d "$PACT_DIR" ]; then
  echo "Pact directory $PACT_DIR does not exist. Exiting."
  exit 1
fi

# Loop through build/pacts directory to identify the consumers and map them to environment
for pactfile in "$PACT_DIR"/*.json; do
  if [[ -f "$pactfile" ]]; then
    filename=$(basename "$pactfile")

    # Extract the consumer name from filename (before the first '-')
    consumer="${filename%%-*}"

    # map the consumer to environment
     pact-broker create-version-tag \
        --pacticipant "$consumer" \
        --version "$GIT_COMMIT" \
        --tag "$PACT_ENV" \
        --broker-base-url "$PACT_BROKER_URL" \
        --broker-token "$PACT_BROKER_TOKEN"
  fi
done


