FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# --- Observabilidad (OpenTelemetry -> SigNoz u otro backend OTLP) ---
# El agente se incluye SIEMPRE pero queda APAGADO por defecto (OTEL_SDK_DISABLED=true): no exporta nada
# ni agrega latencia hasta que se configure. Para activarlo, setear en el entorno (Railway):
#   OTEL_SDK_DISABLED=false
#   OTEL_EXPORTER_OTLP_ENDPOINT=<endpoint OTLP de SigNoz Cloud, p. ej. https://ingest.us.signoz.cloud:443>
#   OTEL_EXPORTER_OTLP_HEADERS=signoz-ingestion-key=<tu-ingestion-key>
# (OTEL_SERVICE_NAME ya viene seteado abajo; ver docs/INFRA_PENDIENTE.md.)
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/otel-agent.jar
ENV OTEL_SDK_DISABLED=true
ENV OTEL_SERVICE_NAME=costumi-backend

ENTRYPOINT ["sh","-c","java -javaagent:/app/otel-agent.jar -Dserver.port=${PORT:-8080} -jar app.jar"]
