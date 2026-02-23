# STEP 1: The "Kitchen" (Build Stage)
# We use a full image that has Maven and JDK 21 installed.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Parent POM and the child folder structure
# We copy POMs first to "cache" dependencies
COPY pom.xml .
COPY ride-service/pom.xml ride-service/

# Download the tools/dependencies before copying the code
RUN mvn dependency:go-offline -B

# Now copy the actual Java source code
COPY ride-service/src ride-service/src

# Compile and package the code into a .jar file
RUN mvn clean package -DskipTests

# STEP 2: The "Delivery Box" (Runtime Stage)
# We switch to a tiny image that ONLY has the Java Runtime (JRE)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the finished "food" from the kitchen stage
COPY --from=build /app/ride-service/target/*.jar ride-service.jar

# The command to start the factory
ENTRYPOINT ["java", "-jar", "ride-service.jar"]