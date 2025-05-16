FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy built JAR file
COPY target/gus-rest-server.jar app.jar

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]