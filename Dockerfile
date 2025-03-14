# Etapa 1: Build da aplicação usando Maven
FROM maven:3.8.1-openjdk-17-slim AS builder
WORKDIR /app
# Copiar o arquivo de definição do projeto e o código-fonte
COPY pom.xml .
COPY src ./src
# Realiza o build do projeto e gera o arquivo JAR
RUN mvn clean package -DskipTests

# Etapa 2: Imagem final para produção
FROM openjdk:17-jdk-alpine
WORKDIR /app
# Copiar o JAR gerado na etapa anterior para a imagem final
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
