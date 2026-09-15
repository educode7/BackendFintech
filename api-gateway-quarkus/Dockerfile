# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY api-gateway-quarkus/pom.xml api-gateway-quarkus/
RUN mvn dependency:go-offline -pl api-gateway-quarkus -am  2>/dev/null || true
COPY shared/ shared/
COPY api-gateway-quarkus/ api-gateway-quarkus/
RUN mvn clean package -pl api-gateway-quarkus -am -DskipTests 

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/api-gateway-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
