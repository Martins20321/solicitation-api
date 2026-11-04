# ==========================================
# Estágio 1: Build (Compilação e Gerenciamento de Dependências)
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Estágio 2: Runtime (Imagem Final de Produção)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
EXPOSE 9001

ENTRYPOINT ["java", "-jar", "app.jar"]