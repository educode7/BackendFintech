# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY docker-maven-settings.xml /root/.m2/settings.xml
ARG QUARKUS_PROFILE=docker
ENV QUARKUS_PROFILE=${QUARKUS_PROFILE}
ARG OIDC_AUTH_SERVER_URL=http://keycloak:8180/realms/wallet
COPY pom.xml .
COPY shared/pom.xml shared/
COPY api-gateway-quarkus/pom.xml api-gateway-quarkus/
COPY payment-service-quarkus/pom.xml payment-service-quarkus/
COPY account-service-quarkus/pom.xml account-service-quarkus/
COPY notification-service-quarkus/pom.xml notification-service-quarkus/
COPY auth-service-quarkus/pom.xml auth-service-quarkus/
COPY shared/ shared/
COPY auth-service-quarkus/ auth-service-quarkus/
RUN --mount=type=cache,target=/root/.m2/repository mvn clean package -pl auth-service-quarkus -am -DskipTests -U -Dquarkus.profile=docker -Dquarkus.oidc.auth-server-url=${OIDC_AUTH_SERVER_URL}

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/auth-service-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8084
ENTRYPOINT ["java", "-Dquarkus.profile=docker"]
