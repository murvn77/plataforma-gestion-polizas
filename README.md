# API de Gestión de Pólizas

API REST para gestionar pólizas de arrendamiento **individuales** y **colectivas** y sus **riesgos**, con renovación por IPC y notificación de cambios a un CORE de seguros simulado.

Construida con **Java 17, Spring Boot 3.5, Spring Data JPA, PostgreSQL y Docker**.

---

## 🚀 Levantar el proyecto (un solo comando)

**Requisito:** Docker Desktop (o Docker Engine con Compose v2).

```bash
docker compose up --build -d
```

Docker Compose levanta dos contenedores en orden:

1. **`polizas-db`** (PostgreSQL 16). Tiene un *healthcheck* con `pg_isready`.
2. **`polizas-api`** (Spring Boot). Arranca **solo cuando la base de datos está lista** (`depends_on: condition: service_healthy`) y expone su propio *healthcheck* en `/actuator/health`.

Para ver el estado de los contenedores:

```bash
docker compose ps
```

Cuando `polizas-api` aparece como `healthy`, el API está listo. Al arrancar se cargan **pólizas de ejemplo** para probar de inmediato.

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

Para bajar todo (y borrar los datos):

```bash
docker compose down -v
```

> **Puerto ocupado:** si el 8080 está en uso, cambie el puerto del host con `API_PORT=9090 docker compose up --build -d` (PowerShell: `$env:API_PORT=9090; docker compose up --build -d`).

### Sin Docker

Con Java 17 y Maven, el API corre con una base **H2 en memoria**:

```bash
mvn spring-boot:run
```

---

## 🔐 Seguridad

Todas las peticiones requieren el header:

```
x-api-key: 123456
```

Sin él, el API responde `401 Unauthorized`. Swagger, `/v3/api-docs` y `/actuator/health` son públicos.

**En Swagger:** haga clic en **Authorize**, escriba `123456` y luego pruebe los endpoints.

---

## 📌 Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/polizas?tipo=&estado=` | Lista pólizas. Filtros opcionales: `tipo` (`INDIVIDUAL`, `COLECTIVA`) y `estado` (`ACTIVA`, `RENOVADA`, `CANCELADA`) |
| `GET` | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza |
| `POST` | `/polizas/{id}/renovar` | Renueva: canon y prima se incrementan según el IPC y el estado pasa a `RENOVADA` |
| `POST` | `/polizas/{id}/cancelar` | Cancela la póliza y **todos sus riesgos** |
| `POST` | `/polizas/{id}/riesgos` | Agrega un riesgo (**solo pólizas colectivas**) |
| `POST` | `/riesgos/{id}/cancelar` | Cancela un riesgo de una póliza colectiva |
| `POST` | `/core-mock/evento` | Mock del CORE: registra en logs el evento recibido |

### Ejemplos con curl

```bash
# Listar pólizas colectivas activas
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

# Renovar la póliza 1
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

# Agregar un riesgo a la póliza colectiva 3
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"direccionInmueble":"Calle 100 # 15-20, Apto 603","arrendatario":"Camila Ortiz","arrendador":"Inversiones Ríos S.A.S."}' \
  http://localhost:8080/polizas/3/riesgos

# Mock del CORE
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"evento":"ACTUALIZACION","polizaId":555}' \
  http://localhost:8080/core-mock/evento
```

### Datos de ejemplo

| Id | Tipo | Estado | Tomador | Riesgos |
|---|---|---|---|---|
| 1 | INDIVIDUAL | ACTIVA | Ana María Gómez | 1 |
| 2 | INDIVIDUAL | CANCELADA | Jorge Pérez | 1 (cancelado) |
| 3 | COLECTIVA | ACTIVA | Inmobiliaria Los Andes S.A.S. | 3 |
| 4 | COLECTIVA | ACTIVA | Administración Edificio Torres del Parque | 2 |

---

## 📏 Reglas de negocio

| Regla | Dónde se aplica |
|---|---|
| Una póliza individual tiene exactamente **1 riesgo** | `Poliza.crearIndividual` la crea con su riesgo; `agregarRiesgo` rechaza individuales |
| Una póliza colectiva tiene **uno o muchos** riesgos | `Poliza.crearColectiva` exige al menos uno |
| **No se puede renovar** una póliza cancelada | `Poliza.renovar` |
| **Cancelar la póliza cancela todos sus riesgos** | `Poliza.cancelar` |
| Agregar un riesgo **exige validar el tipo** de póliza | `Poliza.agregarRiesgo` |
| Prima = canon mensual × meses de vigencia | `Poliza.calcularPrima` |
| Renovación: nuevo canon = canon × (1 + IPC/100), por el mismo periodo inicial | `Poliza.renovar` |

Las violaciones de reglas responden **`409 Conflict`** con un cuerpo [ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457):

```json
{
  "type": "about:blank",
  "title": "Regla de negocio no cumplida",
  "status": 409,
  "detail": "No se puede renovar una póliza cancelada",
  "instance": "/polizas/2/renovar"
}
```

| Código | Cuándo |
|---|---|
| `400` | Datos inválidos (campos vacíos, filtros con valores no permitidos, JSON mal formado) |
| `401` | Falta `x-api-key` o es incorrecta |
| `404` | La póliza o el riesgo no existe |
| `409` | Se viola una regla de negocio o hay un conflicto de concurrencia |
| `500` | Error inesperado (sin exponer detalles internos; el detalle va al log) |

---

## 🏗️ Arquitectura

Arquitectura en capas, con un **puerto hexagonal** para la integración con el CORE:

```
controller/   → Recibe HTTP, valida el formato (@Valid) y delega. Sin lógica de negocio.
service/      → Orquesta casos de uso: carga, invoca al dominio y publica eventos.
model/        → Entidades y reglas de negocio (Poliza es la raíz del agregado).
repository/   → Acceso a datos con Spring Data JPA.
client/       → Puerto CoreClient + adaptador hacia el CORE (hoy, el mock HTTP).
dto/          → Objetos de entrada y salida del API (no se exponen las entidades).
exception/    → Excepciones de negocio y manejador global de errores.
config/       → API key, Swagger, procesamiento asíncrono y datos de ejemplo.
```

### Flujo de una renovación

```
POST /polizas/1/renovar
  → PolizaController
  → PolizaService.renovar()            @Transactional
      → poliza.renovar(ipc)            reglas en el dominio
      → publica EventoCore(RENOVACION)
  ← commit en la base de datos
  ← respuesta 200 al cliente
  → CoreEventListener                  AFTER_COMMIT + @Async
      → CoreClient.enviar()            POST /core-mock/evento
      → [CORE-MOCK] Se intentó enviar al CORE: evento=RENOVACION, polizaId=1
```

### Decisiones de diseño

- **Modelo de dominio rico (DDD):** las reglas viven en `Poliza`, la **raíz del agregado**. Los riesgos solo se crean y se cancelan a través de ella: el constructor y `cancelar()` de `Riesgo` son *package-private*, y `getRiesgos()` devuelve una lista no modificable. Así las reglas no se pueden saltar.
- **Static factory methods** (`crearIndividual`, `crearColectiva`): cada tipo de póliza nace válido.
- **Notificación al CORE después del commit y asíncrona** (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`): solo se notifican cambios confirmados y el cliente no espera al CORE. Si el CORE falla, se registra el error sin revertir la operación.
- **Puerto `CoreClient` (hexagonal):** los servicios dependen de la interfaz, no del transporte. El mock se puede reemplazar por un adaptador real hacia WebLogic sin tocar la lógica de negocio. Los tests lo sustituyen con un *mock*.
- **Optimistic locking** (`@Version`): evita que dos actualizaciones simultáneas se pisen; el conflicto responde `409`.
- **`BigDecimal`** con redondeo `HALF_UP` a 2 decimales para todos los valores monetarios.
- **`FetchType.LAZY`** por defecto y **`JOIN FETCH`** donde se necesita la relación (cancelar un riesgo carga su póliza en una sola consulta).
- **Configuración externalizada:** IPC, API key, URL del CORE y credenciales de base de datos se pasan por variables de entorno.
- **Docker:** imagen *multi-stage* (compila con Maven y ejecuta solo el JRE en Alpine), usuario no root, *healthchecks* y arranque ordenado.

### Supuestos

1. El riesgo de una póliza **individual** no se cancela por separado; se cancela la póliza.
2. No se puede cancelar el **último riesgo activo** de una colectiva; en ese caso se cancela la póliza.
3. La renovación inicia el día en que termina la vigencia anterior y dura los mismos meses.
4. El **IPC** es un parámetro configurable (`IPC_PORCENTAJE`, por defecto 5,20 %).
5. Una póliza renovada puede volver a renovarse.
6. Se usan las rutas exactas del enunciado. En el diseño de arquitectura se propone versionar en la URI (`/api/v1/...`).
7. El diseño de referencia usa **Oracle**; aquí se usa **PostgreSQL** por ser más liviano para ejecutar localmente. Con JPA, el cambio es de driver y configuración.

---

## 🧪 Tests

```bash
mvn test
```

| Suite | Qué cubre |
|---|---|
| `PolizaApiTest` | Los endpoints con MockMvc: API key, filtros, 400/404/409, renovación con IPC, cancelación en cascada, notificación al CORE |
| `PolizaRepositoryTest` | Consultas con filtros opcionales y `JOIN FETCH` |
| `PolizasApplicationTests` | Arranque del contexto de Spring |

---

## 🔭 Qué haría con más tiempo (camino a producción)

- **Patrón Outbox + Kafka** para garantizar que ningún evento hacia el CORE se pierda si la aplicación cae justo después del commit, con **reintentos con backoff, circuit breaker (Resilience4j) y DLQ**.
- **Renovación automática** con un job programado (`@Scheduled` + **ShedLock** para que corra en una sola réplica) e idempotencia.
- **Paginación** en `GET /polizas`.
- **OAuth2/JWT** en un API Gateway en lugar de API key.
- **Migraciones versionadas** con Flyway o Liquibase en lugar de `ddl-auto`.
- **Observabilidad:** logs estructurados con *correlation-id*, métricas con Micrometer/Prometheus y trazas con OpenTelemetry.
- **Separar en microservicios** (Pólizas, Riesgos, Notificaciones, Adapter CORE) según el diseño de arquitectura.

---

## 🛠️ Nota para redes con inspección HTTPS

Si `docker compose up --build` falla con `PKIX path building failed`, un antivirus o proxy corporativo está inspeccionando el tráfico HTTPS. Copie el certificado raíz de esa entidad (formato PEM, extensión `.crt`) en la carpeta `certs/`: el Dockerfile lo importa durante la compilación. Los `.crt` están excluidos de Git.
