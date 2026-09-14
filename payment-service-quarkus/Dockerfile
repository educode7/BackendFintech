# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY payment-service-quarkus/pom.xml payment-service-quarkus/
RUN mvn dependency:go-offline -pl payment-service-quarkus -am -s .mvn/settings.xml 2>/dev/null || true
COPY shared/ shared/
COPY payment-service-quarkus/ payment-service-quarkus/
RUN mvn clean package -pl payment-service-quarkus -am -DskipTests -s .mvn/settings.xml

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/payment-service-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8081 9081
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
