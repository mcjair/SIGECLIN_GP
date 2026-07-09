# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/aeaman-*.jar app.jar
COPY ciex ./ciex
EXPOSE 3001
ENV DB_URL=jdbc:postgresql://postgres:5432/sigeclin?sslmode=prefer
ENV DB_USERNAME=admin
ENV DB_PASSWORD=admin
ENV APP_CRYPTO_KEY=SigeclinSecureKeyDefault32BytesLong
ENTRYPOINT ["java", "-jar", "app.jar"]
