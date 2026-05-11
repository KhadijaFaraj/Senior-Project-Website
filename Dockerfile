# STAGE 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copy the pom and source files into the container
# Note: We are copying from your local 'website' folder
COPY website/pom.xml ./website/
COPY website/src ./website/src/

# 2. Compile the code and build the JAR
WORKDIR /app/website
RUN mvn clean package -DskipTests

# STAGE 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 3. Copy the built JAR from the first stage
COPY --from=build /app/website/target/website-1.0-SNAPSHOT.jar app.jar

# 4. Copy the ML model specifically
# This places it at /app/models/usage_gap.model inside the container
COPY --from=build /app/website/src/main/resources/models/usage_gap.model ./models/usage_gap.model

# Render uses port 8080 by default for Docker
EXPOSE 8080

# 5. Start the Java app
CMD ["java", "-jar", "app.jar"]