# Bicycle Rental API

API REST para gestión de alquiler de bicicletas urbanas. Construida con Spring Boot 3.5 y Java 17.

## URL pública (desplegada en AWS Elastic Beanstalk)

```
http://bicycle-rental-env.eba-fzyiegia.us-east-2.elasticbeanstalk.com
```

> El despliegue es automático con cada `git push` a `main` mediante GitHub Actions.

---

## Arquitectura

Se adoptó una **arquitectura en capas** inspirada en los principios de Clean Architecture, con separación explícita de responsabilidades:

```
src/main/java/com/ceiba/bicycle_rental/
├── api/controller/          → Controladores REST (capa de entrada)
├── application/
│   ├── dto/                 → Objetos de transferencia (Request / Response)
│   └── service/             → Casos de uso y orquestación de la lógica de negocio
├── domain/
│   ├── enums/               → Tipos de datos del dominio (BicycleType, BicycleStatus)
│   ├── model/               → Entidades JPA (Bicycle, Rental)
│   └── repository/          → Interfaces de repositorio (contrato del dominio)
└── infrastructure/
    ├── config/              → Configuración de beans (Clock, DataInitializer)
    └── exception/           → Manejo global de excepciones
```

**Justificación:** Esta separación permite que la lógica de negocio (cálculo de costos, multas, validaciones) viva únicamente en la capa de servicio, sin acoplarse a detalles de transporte HTTP ni de persistencia. Los repositorios son interfaces definidas en el dominio e implementadas por Spring Data JPA en infraestructura, lo que invierte correctamente la dependencia (principio DIP de SOLID).

El `Clock` de Java se inyecta como bean de Spring, lo que permite reemplazarlo por un reloj fijo en pruebas unitarias sin modificar el código de producción.

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 3.5 (Spring Web, Spring Data JPA, Spring Validation) |
| Base de datos | H2 en memoria (se recrea en cada arranque) |
| Persistencia | Hibernate 6 / JPA |
| Utilidades | Lombok |
| Pruebas | JUnit 5 + Mockito + AssertJ |
| Build | Maven Wrapper (`mvnw`) |
| Despliegue | AWS Elastic Beanstalk (Java 17 / Amazon Linux 2023) |
| CI/CD | GitHub Actions (deploy automático en cada push a `main`) |

---

## Requisitos previos

- Java 17 o superior
- Sin dependencias externas adicionales (Maven Wrapper incluido)

---

## Ejecución local

```bash
# Clonar el repositorio
git clone <url-del-repositorio>
cd bicycle-rental

# Ejecutar (Linux / Mac)
./mvnw spring-boot:run

# Ejecutar (Windows)
mvnw.cmd spring-boot:run
```

La API arrancará en `http://localhost:8080`.

Las 5 bicicletas de ejemplo del enunciado se cargan automáticamente al iniciar.

**Consola H2** (solo en desarrollo): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:bicycle_db`
- Usuario: `sa` / Sin contraseña

### Ejecutar pruebas

```bash
# Linux / Mac
./mvnw test

# Windows
mvnw.cmd test
```

---

## Supuestos documentados

1. **Nombres de tipos de bicicleta:** La especificación usa `MONTAÑA` y `ELÉCTRICA`. Para garantizar compatibilidad ASCII en todos los entornos, los valores del enum son `MONTANA` y `ELECTRICA`. Los clientes deben enviar estos valores en el JSON.

2. **Hora de inicio:** La hora de inicio del alquiler la establece el servidor en el momento de la petición (`LocalDateTime.now()`), no el cliente.

3. **Tiempo mínimo cobrado:** Si el tiempo de uso es menor a 1 minuto (caso extremo), el costo base es $0. En la práctica esto no ocurre, pero `Math.ceil(0/60.0) = 0`.

4. **Multa con retraso exacto de 0 minutos:** Devolver exactamente a la hora estimada no genera multa (se usa `isAfter`, no `isAfterOrEqual`).

---

## Endpoints

### Bicicletas

#### Registrar bicicleta
```bash
POST /api/bikes
Content-Type: application/json

{
  "code": "BIC-006",
  "type": "URBANA",
  "status": "DISPONIBLE"
}
```

#### Consultar bicicletas disponibles (RF-04)
```bash
# Todas las disponibles
GET /api/bikes/available

# Filtrar por tipo
GET /api/bikes/available?type=MONTANA
GET /api/bikes/available?type=URBANA
GET /api/bikes/available?type=ELECTRICA
```

#### Consultar bicicleta por código
```bash
GET /api/bikes/BIC-001
```

#### Historial de alquileres de una bicicleta (RF-05)
```bash
GET /api/bikes/BIC-002/rentals
```

---

### Alquileres

#### Iniciar un alquiler (RF-02)
```bash
POST /api/rentals
Content-Type: application/json

{
  "bicycleCode": "BIC-001",
  "clientName": "Maria Lopez",
  "estimatedHours": 3
}
```

#### Finalizar un alquiler (RF-03)
```bash
PUT /api/rentals/1/finish
```

---

## Ejemplos de flujo completo (curl)

Reemplaza `BASE_URL` por `http://localhost:8080` en local o por la URL de AWS en producción.

```bash
BASE_URL=http://bicycle-rental-env.eba-fzyiegia.us-east-2.elasticbeanstalk.com

# 1. Ver bicicletas disponibles
curl $BASE_URL/api/bikes/available

# 2. Iniciar alquiler de BIC-002 (MONTANA)
curl -s -X POST $BASE_URL/api/rentals \
  -H "Content-Type: application/json" \
  -d '{"bicycleCode":"BIC-002","clientName":"Juan Garcia","estimatedHours":2}'

# 3. Finalizar el alquiler (usar el id devuelto en el paso 2)
curl -X PUT $BASE_URL/api/rentals/1/finish

# 4. Ver historial de BIC-002
curl $BASE_URL/api/bikes/BIC-002/rentals

# 5. Intentar alquilar una bicicleta en mantenimiento (debe fallar con 409)
curl -s -X POST $BASE_URL/api/rentals \
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

- Bicicleta: MONTANA ($5.000/h)
- Estimado: 2 horas
- Uso real: 3h 20min

| Concepto | Cálculo | Valor |
|---|---|---|
| Horas reales (↑) | ceil(200/60) = 4h | — |
| Costo base | 4 × $5.000 | $20.000 |
| Retraso | 3h20m − 2h = 1h20m → ceil = 2h | — |
| Multa | 2 × ($5.000 × 50%) | $5.000 |
| **Total** | | **$25.000** |

---

## Manejo de errores

| Situación | HTTP |
|---|---|
| Bicicleta no disponible (RN-04) | 409 Conflict |
| Alquiler ya finalizado (RN-05) | 409 Conflict |
| Recurso no encontrado | 404 Not Found |
| Validación de entrada fallida | 400 Bad Request |
| Error interno | 500 Internal Server Error |

Todas las respuestas de error incluyen `status`, `message` y `timestamp`.
