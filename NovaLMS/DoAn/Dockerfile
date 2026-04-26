# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user and ensure /app is writable
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app

USER spring:spring

# Copy the jar and SQL seed file from build stage
COPY --from=build /app/target/DoAn-0.0.1-SNAPSHOT.jar app.jar
COPY massive_seed_v2.sql .

# Render scans EXPOSE to detect the port. Spring Boot will use $PORT from properties.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
