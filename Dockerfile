# Multi-stage Dockerfile for Billora (Spring Boot 3 + Java 21)
# Stage 1: Build application
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper and POM to leverage Docker layer caching for dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy application source code and build the production JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal and secure runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a dedicated non-root user and group
RUN groupadd -r billora && useradd -r -g billora billora

# Copy compiled executable JAR from builder stage
COPY --from=builder --chown=billora:billora /app/target/*.jar app.jar

USER billora

# Dynamic PORT environment variable for Render (defaults to 8080 if unset)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
