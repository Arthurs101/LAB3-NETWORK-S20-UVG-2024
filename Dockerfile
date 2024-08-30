# Maven image
FROM maven:3.9.4-eclipse-temurin-17
# Working app directory
WORKDIR /app
# Copy dependencies
COPY ./lab3-protocols/pom.xml .
# Copy code source
COPY ./lab3-protocols/src ./src
# Compile the application
RUN mvn clean install
# Execute the JAR file
CMD ["java", "-jar", "target/lab3-protocols-1.0-SNAPSHOT.jar"]