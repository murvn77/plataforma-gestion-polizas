FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Certificados raíz opcionales (ej. antivirus o proxy corporativo que inspecciona HTTPS).
# Si certs/ no tiene archivos .crt, no se importa nada.
COPY certs/ /tmp/certs/
RUN for cert in /tmp/certs/*.crt; do \
      [ -f "$cert" ] && keytool -importcert -noprompt -cacerts -storepass changeit \
        -alias "$(basename "$cert")" -file "$cert"; \
    done; true

# Dependencias en una capa aparte: se reutiliza mientras el pom no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/polizas-api-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
