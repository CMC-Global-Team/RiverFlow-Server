# Stage 1: Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with optimizations
RUN mvn clean package -DskipTests -B \
    && rm -rf target/*-sources.jar target/*-javadoc.jar target/classes target/test-classes

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set JVM options optimized for fast startup and low memory
ENV JAVA_OPTS="\
-Xmx400m \
-Xms200m \
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:+UseSerialGC \
-XX:TieredStopAtLevel=1 \
-Xss256k \
-XX:MaxMetaspaceSize=128m \
-Djava.security.egd=file:/dev/./urandom \
-Dspring.jmx.enabled=false \
-Dspring.main.lazy-initialization=true"

# Health check with faster intervals
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

