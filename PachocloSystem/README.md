# PachocloSystem

API REST (Spring Boot 4.1.1) para gestionar pacientes, trabajadores y el historial clínico de un hospital.
Migrada desde una aplicación Swing con arquitectura MVC.

## Requisitos

- **Java 25** (`JAVA_HOME` debe apuntar a un JDK 25; el `pom.xml` fija `java.version=25`).
- No hace falta instalar Maven: se usa el wrapper incluido.

## Cómo arrancar

```bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd / PowerShell)
```

La API queda en `http://localhost:8080`. Otros comandos útiles:

```bash
./mvnw clean compile          # compilar
./mvnw test                   # ejecutar pruebas
```

> Los datos se guardan **en memoria**: se pierden al reiniciar la aplicación.

## Estructura

```
src/main/java/com/pachoclosystem/demo/
├── controller/   endpoints REST
├── service/      lógica de negocio y validaciones
├── repository/   almacenamiento en memoria
├── model/        entidades (Paciente, Doctor, Enfermero, RegistroClinico, ...)
├── dto/          requests y responses
└── exception/    errores y @RestControllerAdvice
```

## Endpoints

### Pacientes — `/api/pacientes`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/pacientes?q=` | Lista pacientes; `q` filtra por id o nombre | 200 |
| GET | `/api/pacientes/{id}` | Obtiene un paciente | 200 / 404 |
| POST | `/api/pacientes` | Registra un paciente `{nombre, edad, habitacion}` | 201 / 400 |
| PUT | `/api/pacientes/{id}` | Edita nombre, edad y habitación | 200 / 400 / 404 |
| PATCH | `/api/pacientes/{id}/habitacion` | Cambia solo la habitación `{habitacion}` | 200 / 400 / 404 |
| DELETE | `/api/pacientes/{id}` | Elimina un paciente | 204 / 404 |

### Trabajadores — `/api/trabajadores`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/trabajadores?q=` | Lista trabajadores; `q` filtra por id o nombre | 200 |
| GET | `/api/trabajadores/{id}` | Obtiene un trabajador | 200 / 404 |
| POST | `/api/trabajadores` | Registra `{nombre, rol: "Doctor"\|"Enfermero", especialidad?, nivelExperiencia?}` | 201 / 400 |
| PUT | `/api/trabajadores/{id}` | Edita un trabajador (el rol no puede cambiar) | 200 / 400 / 404 |
| DELETE | `/api/trabajadores/{id}` | Elimina un trabajador | 204 / 404 |

Un Doctor requiere `especialidad`; un Enfermero requiere `nivelExperiencia` (`NOVATO`, `PRINCIPIANTE`, `AVANZADO`).

### Historial clínico

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/historial?filtro=&q=` | Todos los registros ordenados por fecha. `filtro`: `todos` (defecto), `paciente` o `autor` | 200 / 400 |
| GET | `/api/pacientes/{id}/historial?q=` | Registros de un paciente; `q` filtra por autor | 200 / 404 |
| POST | `/api/pacientes/{id}/historial` | Agrega un registro | 201 / 400 / 404 |

Cuerpo de `POST /api/pacientes/{id}/historial`:

```json
{ "tipo": "DIAGNOSTICO", "idAutor": "DOC-0001", "contenido": "Hipertensión leve" }
```

`tipo` puede ser `DIAGNOSTICO`, `EVOLUCION`, `MEDICACION` o `SIGNOS_VITALES`. Para `SIGNOS_VITALES` se envía `signosVitales` en lugar de `contenido`:

```json
{
  "tipo": "SIGNOS_VITALES", "idAutor": "ENF-0001",
  "signosVitales": {
    "temperatura": 36.5, "frecCardiaca": 80,
    "presionSistolica": 120, "presionDiastolica": 80,
    "frecRespiratoria": 16, "saturacion": 98, "observaciones": "estable"
  }
}
```

## Errores

Los errores devuelven un cuerpo uniforme:

```json
{ "status": 400, "error": "Bad Request", "mensajes": ["La especialidad es obligatoria."] }
```
