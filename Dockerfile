 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.1

#FROM crmdvrepo01.azurecr.io/registry.hub.docker.com/library/openjdk:21-jdk-slim
FROM openjdk:21-jdk-slim


COPY ./build/libs/stg-track-my-case-api-springboot-template.jar /opt/app/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 4550
RUN chmod 755 /opt/app/stg-track-my-case-api-springboot-template.jar
CMD [ "java", "-jar", "/opt/app/stg-track-my-case-api-springboot-template.jar" ]
