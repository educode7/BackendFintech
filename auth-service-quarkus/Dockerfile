# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY auth-service-quarkus/pom.xml auth-service-quarkus/
RUN mvn dependency:go-offline -pl auth-service-quarkus -am  2>/dev/null || true
COPY shared/ shared/
COPY auth-service-quarkus/ auth-service-quarkus/
RUN mvn clean package -pl auth-service-quarkus -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/auth-service-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
