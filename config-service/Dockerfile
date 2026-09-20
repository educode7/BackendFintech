# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY shared/pom.xml shared/
COPY api-gateway-quarkus/pom.xml api-gateway-quarkus/
COPY payment-service-quarkus/pom.xml payment-service-quarkus/
COPY account-service-quarkus/pom.xml account-service-quarkus/
COPY notification-service-quarkus/pom.xml notification-service-quarkus/
COPY auth-service-quarkus/pom.xml auth-service-quarkus/
RUN mvn -N install -q

COPY config-service/ config-service/
RUN mvn -f config-service/pom.xml -DskipTests package -q

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /build/config-service/target/config-service-1.0.0.jar app.jar
COPY config-repo /config-repo
EXPOSE 8888

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-Xms128m", \
    "-Xmx256m", \
    "-jar", "/app/app.jar"]
