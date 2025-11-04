# ===== FASE 1: Build con Maven =====
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copia il POM e scarica le dipendenze
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia il codice sorgente e compila
COPY src ./src
RUN mvn clean package -DskipTests

# ===== FASE 2: Runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia il JAR costruito nella fase di build
COPY --from=builder /app/target/*.jar /app/app.jar

# Espone la porta 8080
EXPOSE 10000

# Comando di avvio
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


