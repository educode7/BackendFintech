# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# Cache parent + shared module first so Docker layer cache is reused when only one service changes.
COPY pom.xml ./
COPY shared shared
RUN mvn -pl shared -am -DskipTests install -q

COPY api-gateway api-gateway
RUN mvn -pl api-gateway -am -DskipTests package -q

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080

# ── JVM Tuning ──────────────────────────────────────────────────────────
# ZGC (generational): sub-millisecond GC pauses for latency-critical workloads.
# API gateway is the entry point — GC pauses here affect every single request.
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-XX:MaxGCPauseMillis=1", \
    "-Xms128m", \
    "-Xmx256m", \
    "-Xlog:gc*,gc+heap=debug,gc+phases=debug:file=/tmp/gc.log:time,level,tags:filecount=5,filesize=10m", \
    "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=/tmp/flight.jfr,maxage=1h,maxsize=250m,settings=default", \
    "-jar", "/app/app.jar"]
