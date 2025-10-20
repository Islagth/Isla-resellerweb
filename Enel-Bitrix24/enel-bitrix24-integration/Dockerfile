# Fase 1: build con Maven
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder
WORKDIR /app

# Copia il POM e scarica le dipendenze
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Compila il progetto
COPY src ./src
RUN mvn clean package -DskipTests

# Fase 2: runtime (solo JDK)
FROM openjdk:21-jdk-slim
WORKDIR /app

# Comando di avvio (sostituisci il nome del jar corretto)
CMD ["java", "-jar", "target/enel-bitrix24-integration-0.0.1-SNAPSHOT.jar"]

# Espone la porta 8080
EXPOSE 8080

# Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]

