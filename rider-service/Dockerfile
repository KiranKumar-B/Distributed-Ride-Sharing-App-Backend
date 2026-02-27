# STEP 1: The "Kitchen" (Build Stage)
# We use a full image that has Maven and JDK 21 installed.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Parent POM and the child folder structure
# We copy POMs first to "cache" dependencies
COPY pom.xml .
COPY rider-service/pom.xml rider-service/
# In a multi-module setup, the rider-service depends on the parent pom.xml, and the parent pom.xml "owns" the driver-service. Similarly copy code too
COPY driver-service/pom.xml driver-service/

# Download the tools/dependencies before copying the code
RUN mvn dependency:go-offline -B

# Now copy the actual Java source code
COPY rider-service/src rider-service/src
COPY driver-service/src driver-service/src

# Compile and package the code into a .jar file
RUN mvn clean package -pl rider-service -am -DskipTests

# STEP 2: The "Delivery Box" (Runtime Stage)
# We switch to a tiny image that ONLY has the Java Runtime (JRE)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the finished "food" from the kitchen stage
COPY --from=build /app/rider-service/target/*.jar app.jar

# The command to start the factory
ENTRYPOINT ["java", "-jar", "app.jar"]