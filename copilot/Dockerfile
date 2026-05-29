# Use the official Eclipe Termurin JDK 21 as the base image
FROM eclipse-temurin:21-jdk

# set the working directory inside the container (cd /app)
WORKDIR /app

# Copy the built JAR file from the target directory into the container
COPY target/copilot-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the Spring Boot application listens on
EXPOSE 8080

# Configure the container to run the Spring Boot application on startup
ENTRYPOINT ["java", "-jar", "app.jar"]