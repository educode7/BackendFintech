# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY docker-maven-settings.xml /root/.m2/settings.xml
ARG QUARKUS_PROFILE=docker
ENV QUARKUS_PROFILE=${QUARKUS_PROFILE}
ARG WALLET_GATEWAY_ROUTES_PAYMENT_SERVICE_URL=http://payment-service:8081
ARG WALLET_GATEWAY_ROUTES_ACCOUNT_SERVICE_URL=http://account-service:8082
ARG WALLET_GATEWAY_ROUTES_NOTIFICATION_SERVICE_URL=http://notification-service:8083
ARG WALLET_GATEWAY_ROUTES_AUTH_SERVICE_URL=http://auth-service:8084
ARG OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV WALLET_GATEWAY_ROUTES_PAYMENT_SERVICE_URL=${WALLET_GATEWAY_ROUTES_PAYMENT_SERVICE_URL}
ENV WALLET_GATEWAY_ROUTES_ACCOUNT_SERVICE_URL=${WALLET_GATEWAY_ROUTES_ACCOUNT_SERVICE_URL}
ENV WALLET_GATEWAY_ROUTES_NOTIFICATION_SERVICE_URL=${WALLET_GATEWAY_ROUTES_NOTIFICATION_SERVICE_URL}
ENV WALLET_GATEWAY_ROUTES_AUTH_SERVICE_URL=${WALLET_GATEWAY_ROUTES_AUTH_SERVICE_URL}
COPY pom.xml .
COPY shared/pom.xml shared/
COPY api-gateway-quarkus/pom.xml api-gateway-quarkus/
COPY payment-service-quarkus/pom.xml payment-service-quarkus/
COPY account-service-quarkus/pom.xml account-service-quarkus/
COPY notification-service-quarkus/pom.xml notification-service-quarkus/
COPY auth-service-quarkus/pom.xml auth-service-quarkus/
COPY shared/ shared/
COPY api-gateway-quarkus/ api-gateway-quarkus/
RUN --mount=type=cache,target=/root/.m2/repository mvn clean package -pl api-gateway-quarkus -am -DskipTests -U -Dquarkus.profile=docker -Dquarkus.otel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT} -Dquarkus.otel.exporter.otlp.protocol=grpc

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/api-gateway-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8080
ENTRYPOINT ["java", "-Dquarkus.profile=docker"]
