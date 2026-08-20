# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY shared shared
RUN mvn -pl shared -am -DskipTests install -q

COPY config-service config-service
RUN mvn -pl config-service -am -DskipTests package -q

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /build/config-service/target/*.jar app.jar
COPY config-repo /config-repo
EXPOSE 8888

# ── JVM Tuning ──────────────────────────────────────────────────────────
# Config server is lightweight — minimal heap, no ZGC needed.
# But we keep ZGC for consistency and fast startup.
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-Xms128m", \
    "-Xmx256m", \
    "-jar", "/app/app.jar"]
