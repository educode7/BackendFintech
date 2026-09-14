# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/
COPY account-service-quarkus/pom.xml account-service-quarkus/
RUN mvn dependency:go-offline -pl account-service-quarkus -am -s .mvn/settings.xml 2>/dev/null || true
COPY shared/ shared/
COPY account-service-quarkus/ account-service-quarkus/
RUN mvn clean package -pl account-service-quarkus -am -DskipTests -s .mvn/settings.xml

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/account-service-quarkus/target/quarkus-app/ ./quarkus-app/
EXPOSE 8082 9082
ENTRYPOINT ["java", "-jar", "quarkus-app/quarkus-run.jar"]
