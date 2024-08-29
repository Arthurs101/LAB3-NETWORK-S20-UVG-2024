# Use an official Maven image as a parent image
FROM maven:3.9.4-eclipse-temurin-17

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and source code to the container
COPY ./lab3-protocols/pom.xml .
COPY ./lab3-protocols/src ./src

# Build the application
RUN mvn package

# Specify the JAR file that will be executed
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.networks.Main"]

# docker build -t lab3-networks .
# docker run lab3-networks 