# Bicycle Rental API

API REST para gestión de alquiler de bicicletas urbanas. Construida con Spring Boot 3.5 y Java 17.

## URL pública (AWS Elastic Beanstalk)

```
http://bicycle-rental-env.eba-fzyiegia.us-east-2.elasticbeanstalk.com
```

> El despliegue es automático con cada `git push` a `main` mediante GitHub Actions (CI/CD).

---

## Arquitectura

Se adoptó una **arquitectura en capas** inspirada en Clean Architecture, con separación explícita de responsabilidades:

```
src/main/java/com/ceiba/bicycle_rental/
├── api/controller/          → Controladores REST (capa de entrada HTTP)
├── application/
│   ├── dto/                 → Objetos de transferencia (Request / Response)
│   └── service/             → Casos de uso y lógica de negocio
├── domain/
│   ├── enums/               → BicycleType, BicycleStatus
│   ├── model/               → Entidades JPA (Bicycle, Rental)
│   └── repository/          → Interfaces de repositorio (contrato del dominio)
└── infrastructure/
    ├── config/              → Configuración de beans (Clock, DataInitializer)
    └── exception/           → Manejo global de excepciones
```

**Justificación:**
- La lógica de negocio (cálculo de costos, multas, validaciones) vive únicamente en la capa de servicio, sin acoplarse a HTTP ni a persistencia.
- Los repositorios son interfaces definidas en el dominio e implementadas por Spring Data JPA, aplicando el principio de inversión de dependencias (DIP de SOLID).
- El `Clock` de Java se inyecta como bean de Spring, permitiendo reemplazarlo por un reloj fijo en pruebas unitarias sin modificar el código de producción.
- Se aplicaron principios SOLID y DRY: el cálculo de multas está encapsulado en un único método privado, las validaciones de estado están centralizadas y los DTOs separan la representación de las entidades del dominio.

---

## Tecnologías y dependencias

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 3.5 |
| API | Spring Web (REST) |
| Persistencia | Spring Data JPA + Hibernate 6 |
| Base de datos | H2 en memoria |
| Validación | Spring Boot Validation (Jakarta) |
| Utilidades | Lombok |
| Pruebas | JUnit 5 + Mockito + AssertJ |
| Build | Maven Wrapper (`mvnw`) |
| Despliegue | AWS Elastic Beanstalk (Corretto 17 / Amazon Linux 2023) |
| CI/CD | GitHub Actions |

---

## Requisitos previos

- Java 17 o superior
- Maven (o usar el Maven Wrapper incluido, sin instalación adicional)

---

## Ejecución local

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd bicycle-rental

# 2. Ejecutar la aplicación
# Linux / Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

La API arrancará en `http://localhost:8080`.

Las 5 bicicletas del enunciado se cargan automáticamente al iniciar:

| Código | Tipo | Estado inicial |
|---|---|---|
| BIC-001 | URBANA | DISPONIBLE |
| BIC-002 | MONTANA | DISPONIBLE |
| BIC-003 | ELECTRICA | DISPONIBLE |
| BIC-004 | MONTANA | EN_MANTENIMIENTO |
| BIC-005 | URBANA | DISPONIBLE |

**Consola H2** (solo desarrollo): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:bicycle_db`
- Usuario: `sa` / Sin contraseña

### Ejecutar pruebas unitarias

```bash
# Linux / Mac
./mvnw test

# Windows
mvnw.cmd test
```

21 pruebas unitarias cubren: cálculo de costos, multas, validaciones de estado y tarifas por tipo.

---

## Endpoints disponibles

### Bicicletas

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/bikes` | Listar todas las bicicletas |
| `GET` | `/api/bikes?status=DISPONIBLE` | Filtrar por estado |
| `GET` | `/api/bikes/available` | Solo bicicletas disponibles |
| `GET` | `/api/bikes/available?type=MONTANA` | Disponibles filtradas por tipo |
| `GET` | `/api/bikes/{code}` | Buscar bicicleta por código |
| `POST` | `/api/bikes` | Registrar nueva bicicleta |
| `PUT` | `/api/bikes/{code}/status` | Actualizar estado de una bicicleta |
| `GET` | `/api/bikes/{code}/rentals` | Historial de alquileres |

### Alquileres

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/rentals` | Iniciar un alquiler |
| `PUT` | `/api/rentals/{id}/finish` | Finalizar un alquiler |

---

## Ejemplos de peticiones (curl)

Usa `BASE_URL=http://localhost:8080` en local o la URL de AWS en producción.

```bash
BASE_URL=http://bicycle-rental-env.eba-fzyiegia.us-east-2.elasticbeanstalk.com

# Listar todas las bicicletas
curl $BASE_URL/api/bikes

# Listar solo las disponibles
curl $BASE_URL/api/bikes/available

# Filtrar disponibles por tipo
curl "$BASE_URL/api/bikes/available?type=MONTANA"

# Filtrar todas por estado
curl "$BASE_URL/api/bikes?status=EN_MANTENIMIENTO"

# Buscar bicicleta por código
curl $BASE_URL/api/bikes/BIC-001

# Registrar nueva bicicleta
curl -X POST $BASE_URL/api/bikes \
  -H "Content-Type: application/json" \
  -d '{"code":"BIC-006","type":"ELECTRICA","status":"DISPONIBLE"}'

# Cambiar estado de una bicicleta (ej: sacar de mantenimiento)
curl -X PUT $BASE_URL/api/bikes/BIC-004/status \
  -H "Content-Type: application/json" \
  -d '{"status":"DISPONIBLE"}'

# Iniciar alquiler
curl -X POST $BASE_URL/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"bicycleCode":"BIC-002","clientName":"Juan Garcia","estimatedHours":2}'

# Finalizar alquiler (reemplazar 1 por el id devuelto al iniciar)
curl -X PUT $BASE_URL/api/rentals/1/finish

# Ver historial de alquileres de una bicicleta
curl $BASE_URL/api/bikes/BIC-002/rentals

# Error esperado: alquilar bicicleta en mantenimiento (409 Conflict)
curl -X POST $BASE_URL/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"bicycleCode":"BIC-004","clientName":"Ana","estimatedHours":1}'
```

---

## Reglas de negocio implementadas

| Regla | Descripción |
|---|---|
| RN-01 | Tarifas: URBANA $3.500/h · MONTANA $5.000/h · ELECTRICA $7.500/h |
| RN-02 | Costo base = horas reales redondeadas al alza × tarifa |
| RN-03 | Multa = horas de retraso redondeadas al alza × (tarifa × 50%) |
| RN-04 | Solo se alquilan bicicletas en estado DISPONIBLE |
| RN-05 | No se puede finalizar un alquiler inexistente o ya finalizado |

### Ejemplo de cálculo (del enunciado)

Bicicleta MONTANA · Estimado: 2h · Uso real: 3h 20min

| Concepto | Cálculo | Valor |
|---|---|---|
| Horas reales (↑) | ceil(200 min / 60) = 4h | — |
| Costo base | 4h × $5.000 | $20.000 |
| Retraso | 3h20m − 2h = 1h20m → ceil = 2h | — |
| Multa | 2h × ($5.000 × 50%) | $5.000 |
| **Total** | | **$25.000** |

---

## Manejo de errores

| Situación | HTTP |
|---|---|
| Bicicleta no disponible (RN-04) | 409 Conflict |
| Alquiler ya finalizado (RN-05) | 409 Conflict |
| Recurso no encontrado | 404 Not Found |
| Datos de entrada inválidos | 400 Bad Request |
| Error interno | 500 Internal Server Error |

Todas las respuestas de error tienen el formato:
```json
{
  "status": 409,
  "message": "La bicicleta BIC-004 no esta disponible para alquiler. Estado actual: EN_MANTENIMIENTO",
  "timestamp": "2026-05-29T22:00:00"
}
```

---

## Supuestos documentados

1. **Nombres de tipos:** La especificación usa `MONTAÑA` y `ELÉCTRICA`. Para garantizar compatibilidad ASCII en todos los entornos, los valores son `MONTANA` y `ELECTRICA`.
2. **Hora de inicio:** La establece el servidor al momento de la petición, no el cliente.
3. **Multa con 0 minutos de retraso:** Devolver exactamente en el tiempo estimado no genera multa (condición estricta `isAfter`).
