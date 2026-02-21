  GNU nano 7.2                           Dockerfile                                     
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src/
RUN ./mvnw package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -g 1001 argus && adduser -D -u 1001 -G argus argus
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R argus:argus /app
USER argus

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# CRITICAL: Limit memory to ~1.5GB so it fits in your $20 server
ENV JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]