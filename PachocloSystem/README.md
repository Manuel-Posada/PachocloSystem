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
src/main/java/com/pachoclosystem/pachoclosystem/
├── controller/   endpoints REST
├── service/      lógica de negocio y validaciones
├── repository/   almacenamiento en memoria
├── model/        entidades (Paciente, Doctor, Enfermero, RegistroClinico, Usuario, Rol, ...)
├── dto/          requests y responses
├── config/       configuración (@Bean PasswordEncoder y admin inicial)
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

## Usuarios y roles (en memoria)

Modelo de usuarios como base de la **Fase 2** (Spring Security + JWT). De momento
solo existe el modelo: `Rol` (`ADMIN`, `DOCTOR`, `ENFERMERO`), la entidad
`Usuario`, su repositorio en memoria, el servicio con las reglas de negocio y la
creación de un administrador inicial al arrancar. **No hay autenticación, ni
endpoints de usuarios, ni autorización por rol**: los endpoints actuales siguen
abiertos y responden igual.

### Variables de entorno del administrador inicial

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `ADMIN_USERNAME` | `app.admin.username` | `admin` | Username del administrador inicial (se normaliza a minúsculas; debe cumplir `^[a-z0-9._-]{3,30}$`). |
| `ADMIN_PASSWORD` | `app.admin.password` | *(sin valor en el repo)* | Contraseña en claro del administrador. |

- Si `ADMIN_PASSWORD` **está definida**, se usa tal cual y **nunca se escribe en
  el log**. Si tiene menos de 10 caracteres, la aplicación **no arranca** y
  muestra un mensaje claro con la política.
- Si `ADMIN_PASSWORD` **no está definida**, se genera una contraseña aleatoria
  de 20 caracteres (alfanumérico sin caracteres ambiguos) con `SecureRandom` y se
  escribe **una sola vez** en el log a nivel `WARN`, indicando que es temporal y
  que debe cambiarse.

### Política de contraseña

- Mínimo **10 caracteres**; no se exige ningún requisito de composición. El
  mensaje de error nunca incluye la contraseña.
- Solo se almacena el **hash BCrypt**: la contraseña en claro no se guarda ni
  aparece en `toString()`, en la serialización JSON ni en los logs.
- Un trabajador solo puede tener un usuario; los doctores y enfermeros deben
  estar vinculados a un trabajador existente de su tipo, y los administradores no
  se vinculan a ninguno.

### Advertencia

> Los usuarios se guardan **en memoria**: se pierden al reiniciar la aplicación.
> Si no se define `ADMIN_PASSWORD`, la contraseña aleatoria **queda registrada en
> el log**; esa contraseña solo debe usarse en **desarrollo** y el log no debe
> compartirse. Para cualquier otro entorno, define `ADMIN_PASSWORD` y ten en
> cuenta que, aun así, los datos no sobreviven a un reinicio.
