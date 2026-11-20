# Etapa 1: compilação com Maven e Java 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx768m"
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: imagem final com Java 17 + Agente OpenTelemetry (para Traces no Tempo)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/healthcare.jar app.jar

# Baixa o agente Java do OpenTelemetry para instrumentação automática de HTTP e JDBC (PostgreSQL)
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]