# MedicamentosService

Microservicio REST (Spring Boot 4.1.1) para gestionar el inventario de medicamentos del hospital:
registro, entradas y salidas de stock, alertas de stock bajo y control de vencimientos.

Es independiente del servicio principal `PachocloSystem`: tiene su propio `pom.xml`, su propio
puerto y no comparte código Java con él.

## Requisitos

- **Java 25** (`JAVA_HOME` debe apuntar a un JDK 25; el `pom.xml` fija `java.version=25`).
- No hace falta instalar Maven: se usa el wrapper incluido.

## Cómo arrancar

Desde la carpeta `MedicamentosService/`:

```bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd / PowerShell)
```

La API queda en `http://localhost:8081` (el servicio principal usa el 8080, así que pueden correr a la vez).
Otros comandos útiles:

```bash
./mvnw clean compile          # compilar
./mvnw test                   # ejecutar pruebas
```

> Los datos se guardan **en memoria**: se pierden al reiniciar la aplicación.

## Estructura

```
src/main/java/com/pachoclosystem/medicamentos/
├── controller/   endpoints REST
├── service/      lógica de negocio (stock, vencimientos, duplicados)
├── repository/   IMedicamentoRepository + implementación en memoria
├── model/        Medicamento, DatosMedicamento, Presentacion
├── dto/          requests y responses (records con Bean Validation)
├── config/       @Bean Clock (fecha actual, fijable en pruebas)
└── exception/    errores y @RestControllerAdvice
```

El repositorio en memoria usa `ConcurrentHashMap` y está detrás de la interfaz
`IMedicamentoRepository`: para pasar a base de datos basta con otra implementación.

## Modelo

| Campo | Tipo | Notas |
|---|---|---|
| `idMedicamento` | texto | Generado: `MED-0001`, `MED-0002`, ... |
| `nombre` | texto | Nombre comercial (máx. 80) |
| `principioActivo` | texto | Máx. 80 |
| `presentacion` | enum | `TABLETA`, `CAPSULA`, `JARABE`, `SUSPENSION`, `INYECTABLE`, `CREMA`, `GOTAS`, `OTRO` |
| `concentracion` | texto | Libre, p. ej. `500 mg`, `250 mg/5 ml` (máx. 40) |
| `laboratorio` | texto | Máx. 80 |
| `lote` | texto | Máx. 40 |
| `cantidadStock` | entero | ≥ 0. Se fija al registrar; después solo cambia con entradas y salidas |
| `stockMinimo` | entero | ≥ 0 |
| `fechaVencimiento` | fecha | `AAAA-MM-DD` |
| `ubicacion` | texto | Ubicación de almacenamiento (máx. 80) |
| `stockBajo` | booleano | Solo en respuestas: `cantidadStock <= stockMinimo` |
| `vencido` | booleano | Solo en respuestas: `fechaVencimiento` anterior a hoy |

No puede haber dos medicamentos con el mismo **nombre + concentración + presentación + lote**
(sin distinguir mayúsculas): el intento devuelve `409`.

## Endpoints — `/api/medicamentos`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/medicamentos?q=` | Lista; `q` filtra por id, nombre o principio activo | 200 |
| GET | `/api/medicamentos/{id}` | Obtiene un medicamento | 200 / 404 |
| POST | `/api/medicamentos` | Registra un medicamento (incluye `cantidadStock` inicial) | 201 / 400 / 409 |
| PUT | `/api/medicamentos/{id}` | Edita los datos (todo menos el stock) | 200 / 400 / 404 / 409 |
| DELETE | `/api/medicamentos/{id}` | Elimina un medicamento | 204 / 404 |
| POST | `/api/medicamentos/{id}/entradas` | Suma `{cantidad}` al stock | 200 / 400 / 404 |
| POST | `/api/medicamentos/{id}/salidas` | Resta `{cantidad}` del stock | 200 / 400 / 404 |
| GET | `/api/medicamentos/stock-bajo` | Medicamentos con stock ≤ stock mínimo | 200 |
| GET | `/api/medicamentos/por-vencer?dias=30` | No vencidos que vencen entre hoy y hoy + `dias` (1–365, por defecto 30), del más próximo al más lejano | 200 / 400 |
| GET | `/api/medicamentos/vencidos` | Medicamentos ya vencidos | 200 |

Reglas:

- Se puede **registrar** y **editar** un medicamento con fecha de vencimiento pasada (queda con
  `vencido: true` y aparece en `/vencidos`).
- Una **salida** devuelve `400` si la cantidad supera el stock disponible o si el medicamento está
  vencido; en ambos casos el stock no cambia.
- `cantidad` en entradas y salidas debe ser un entero entre 1 y 1 000 000.

### Ejemplos

Registrar (`POST /api/medicamentos`):

```json
{
  "nombre": "Dolex",
  "principioActivo": "Paracetamol",
  "presentacion": "TABLETA",
  "concentracion": "500 mg",
  "laboratorio": "GSK",
  "lote": "L-2026-001",
  "cantidadStock": 200,
  "stockMinimo": 50,
  "fechaVencimiento": "2027-03-31",
  "ubicacion": "Farmacia - Estante A3"
}
```

Editar (`PUT /api/medicamentos/{id}`): el mismo cuerpo **sin** `cantidadStock`.

Entrada o salida (`POST /api/medicamentos/{id}/entradas` o `/salidas`):

```json
{ "cantidad": 20 }
```

Respuesta de un medicamento:

```json
{
  "idMedicamento": "MED-0001",
  "nombre": "Dolex",
  "principioActivo": "Paracetamol",
  "presentacion": "TABLETA",
  "concentracion": "500 mg",
  "laboratorio": "GSK",
  "lote": "L-2026-001",
  "cantidadStock": 180,
  "stockMinimo": 50,
  "fechaVencimiento": "2027-03-31",
  "ubicacion": "Farmacia - Estante A3",
  "stockBajo": false,
  "vencido": false
}
```

## Autenticación entre servicios

Este servicio no tiene login de usuarios: los usuarios entran por **PachocloSystem** (con JWT), que
reenvía las llamadas aquí. Para que nadie se salte esa puerta, se puede exigir una clave compartida:

| Propiedad | Variable de entorno | Por defecto |
|---|---|---|
| `medicamentos.api-key` | `MEDICAMENTOS_API_KEY` | vacía |

- Con la clave definida, toda petición a `/api/**` debe traer la cabecera `X-Api-Key` con ese valor;
  si falta o no coincide responde **401** ("Falta la clave de servicio o no es válida.").
  PachocloSystem debe tener el mismo `MEDICAMENTOS_API_KEY`.
- Con la clave vacía no se exige nada (solo para desarrollo); al arrancar se registra un WARN.
- Defensa adicional opcional: `server.address=127.0.0.1` para que solo acepte conexiones locales.

```bash
MEDICAMENTOS_API_KEY=clave-local-123 ./mvnw spring-boot:run
curl localhost:8081/api/medicamentos -H "X-Api-Key: clave-local-123"
```

## Errores

Todas las respuestas de error tienen el mismo formato, sin trazas ni detalles internos:

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensajes": ["Stock insuficiente: disponible 10, solicitado 11."]
}
```

| Código | Cuándo |
|---|---|
| 400 | Validación de campos, JSON malformado, stock insuficiente, salida de un vencido, `dias` fuera de rango |
| 401 | Falta la cabecera `X-Api-Key` o no coincide (solo si `MEDICAMENTOS_API_KEY` está definida) |
| 404 | Medicamento o ruta inexistente |
| 405 | Método HTTP no permitido en la ruta (incluye cabecera `Allow`) |
| 409 | Ya existe un medicamento con el mismo nombre, concentración, presentación y lote |
| 415 | `Content-Type` no soportado |
| 500 | Error inesperado (el detalle solo va al log del servidor) |
