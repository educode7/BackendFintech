# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml ./
COPY shared shared
RUN mvn -pl shared -am -DskipTests install -q

COPY notification-service notification-service
RUN mvn -pl notification-service -am -DskipTests package -q

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /build/notification-service/target/*.jar app.jar
EXPOSE 8083

# ── JVM Tuning ──────────────────────────────────────────────────────────
# ZGC (generational): sub-millisecond GC pauses for latency-critical workloads.
# -XX:+ZGenerational: enables generational mode (production-ready since JDK 21).
# -Xmx/-Xms: container-aware; JVM respects cgroup limits automatically in JDK 25.
# GC logs: structured for Prometheus/Grafana ingestion via jfr-jdk25 or jstatd.
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-XX:MaxGCPauseMillis=1", \
    "-Xms256m", \
    "-Xmx512m", \
    "-Xlog:gc*,gc+heap=debug,gc+phases=debug:file=/tmp/gc.log:time,level,tags:filecount=5,filesize=10m", \
    "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=/tmp/flight.jfr,maxage=1h,maxsize=250m,settings=default", \
    "-jar", "/app/app.jar"]
