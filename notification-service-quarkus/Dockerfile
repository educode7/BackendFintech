# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY notification-service-quarkus/pom.xml notification-service-quarkus/
RUN mvn dependency:go-offline -pl notification-service-quarkus -am  2>/dev/null || true
COPY shared/ shared/
COPY notification-service-quarkus/ notification-service-quarkus/
RUN mvn clean package -pl notification-service-quarkus -am -DskipTests 

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/notification-service-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
