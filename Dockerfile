# Dockerfile otimizado para produção
FROM maven:3.8.1-openjdk-17-slim AS builder
WORKDIR /app

# Copiar apenas pom.xml primeiro (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod

# Etapa final - imagem enxuta
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# Copiar JAR
COPY --from=builder /app/target/*.jar app.jar

# Mudar para usuário não-root
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]