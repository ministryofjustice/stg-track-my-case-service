FROM eclipse-temurin:25-jre-jammy

LABEL maintainer="MOJ Strategic Service Transformation Team <STGTransformationTeam@justice.gov.uk>"

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN addgroup --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

WORKDIR /app

COPY --chown=appuser:appgroup docker/stg-track-my-case-service.jar /app/app.jar

USER 2000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

EXPOSE 9999
