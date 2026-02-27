# STEP 1: Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# The context is the ROOT, so we copy the root pom and ALL service poms
# This is required because of the parent-child relationship
COPY pom.xml .
# Add the rider-service too, else we would end up not able to fetch the code kind of error.
#In a multi-module setup, the driver-service depends on the parent pom.xml, and the parent pom.xml "owns" the both services. Similarly copy code too.
COPY rider-service/pom.xml rider-service/
COPY driver-service/pom.xml driver-service/

RUN mvn dependency:go-offline -B

# Copy only the code for THIS service
COPY driver-service/src driver-service/src
COPY rider-service/src rider-service/src

# Build ONLY the driver-service module
RUN mvn clean package -pl driver-service -am -DskipTests

# STEP 2: Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/driver-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]