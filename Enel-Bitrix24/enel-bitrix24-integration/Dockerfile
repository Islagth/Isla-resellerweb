# Usa un'immagine con Java 17 (o 21 se serve)
FROM openjdk:21-jdk-slim

# Imposta una directory di lavoro dentro il container
WORKDIR /app

# Copia il file pom.xml e scarica le dipendenze (per build caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copia tutto il codice sorgente
COPY src ./src

# Compila il progetto
RUN mvn clean package -DskipTests

# Espone la porta 8080
EXPOSE 8080

# Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]

# Comando di avvio (sostituisci il nome del jar corretto)
CMD ["java", "-jar", "target/enel-bitrix24-integration-0.0.1-SNAPSHOT.jar"]

