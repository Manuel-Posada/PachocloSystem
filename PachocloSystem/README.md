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
├── config/       configuración (seguridad, JWT, PasswordEncoder, admin inicial y cliente de medicamentos)
├── security/     JWT, entry point 401/403 y login
├── client/       cliente HTTP de MedicamentosService
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

Un registro `MEDICACION` puede además descontar stock del inventario indicando `idMedicamento` y
`cantidad` (los dos juntos; ver [Integración con MedicamentosService](#integración-con-medicamentosservice)):

```json
{ "tipo": "MEDICACION", "idAutor": "DOC-0001", "contenido": "Paracetamol 500 mg vía oral",
  "idMedicamento": "MED-0001", "cantidad": 2 }
```

### Medicamentos — `/api/medicamentos`

Los datos viven en **MedicamentosService** (otro proceso); estos endpoints los reenvían por HTTP.
Exigen token como el resto de la API.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/medicamentos?q=` | Lista; `q` filtra por id, nombre o principio activo | 200 / 503 |
| GET | `/api/medicamentos/{id}` | Obtiene un medicamento | 200 / 404 / 503 |
| POST | `/api/medicamentos` | Registra un medicamento (con `cantidadStock` inicial) | 201 / 400 / 409 / 503 |
| PUT | `/api/medicamentos/{id}` | Edita los datos (todo menos el stock) | 200 / 400 / 404 / 409 / 503 |
| DELETE | `/api/medicamentos/{id}` | Elimina un medicamento | 204 / 404 / 503 |
| POST | `/api/medicamentos/{id}/entradas` | Suma `{cantidad}` al stock | 200 / 400 / 404 / 503 |
| POST | `/api/medicamentos/{id}/salidas` | Resta `{cantidad}`; 400 si no alcanza o está vencido | 200 / 400 / 404 / 503 |
| GET | `/api/medicamentos/stock-bajo` | Stock igual o menor que el mínimo | 200 / 503 |
| GET | `/api/medicamentos/por-vencer?dias=` | Vencen en los próximos `dias` (1–365, por defecto 30) | 200 / 400 / 503 |
| GET | `/api/medicamentos/vencidos` | Ya vencidos | 200 / 503 |

Los cuerpos y campos son los de MedicamentosService (ver su README).

## Errores

Los errores devuelven un cuerpo uniforme:

```json
{ "status": 400, "error": "Bad Request", "mensajes": ["La especialidad es obligatoria."] }
```

## Integración con MedicamentosService

El servicio principal es la única puerta de entrada (con JWT) al inventario de medicamentos,
que vive en `MedicamentosService` (puerto 8081). Se comunican solo por HTTP; no comparten código.

Configuración (`application.properties`):

| Propiedad | Variable de entorno | Por defecto | Para qué |
|---|---|---|---|
| `medicamentos.url` | `MEDICAMENTOS_URL` | `http://localhost:8081` | URL base del servicio |
| `medicamentos.timeout-conexion` | — | `2s` | Tiempo máximo para conectar |
| `medicamentos.timeout-lectura` | — | `5s` | Tiempo máximo de espera de la respuesta |
| `medicamentos.api-key` | `MEDICAMENTOS_API_KEY` | vacía | Clave compartida que se envía en `X-Api-Key` |

**Autenticación entre servicios.** El usuario se autentica con JWT aquí; MedicamentosService solo
comprueba que quien llama es este servicio, con una clave compartida en la cabecera `X-Api-Key`.
`MEDICAMENTOS_API_KEY` debe tener **el mismo valor en los dos servicios**. Si MedicamentosService
la tiene definida, rechaza (401) cualquier llamada directa sin ella.

**Cómo llegan los errores al cliente:**

| MedicamentosService responde | Este servicio responde |
|---|---|
| 404, 400 o 409 | El mismo código y los mismos mensajes |
| 401/403 (clave distinta), 5xx o una respuesta ilegible | 502 "El servicio de medicamentos respondió de forma inesperada." |
| No responde (caído, conexión rechazada o timeout) | 503 "El servicio de medicamentos no está disponible. Vuelva a intentarlo más tarde." |

**Registros de medicación.** Con `idMedicamento` y `cantidad`, primero se hace la salida de stock
en MedicamentosService y solo si sale bien se guarda el registro; si la salida falla (medicamento
inexistente, stock insuficiente, vencido o servicio caído) no se crea el registro.

> **Límite conocido:** si MedicamentosService aplica la salida pero su respuesta no llega a tiempo
> (timeout de lectura), el stock queda descontado sin registro y el cliente recibe 503. Resolverlo
> del todo (clave de idempotencia o compensación) queda fuera del alcance de este proyecto.

**Prueba manual con los dos servicios** (Git Bash, dos terminales):

```bash
# Terminal 1
cd MedicamentosService && MEDICAMENTOS_API_KEY=clave-local-123 ./mvnw spring-boot:run
# Terminal 2
cd PachocloSystem && MEDICAMENTOS_API_KEY=clave-local-123 ADMIN_PASSWORD=Admin12345 \
  JWT_SECRET=una-clave-jwt-local-de-al-menos-32-bytes ./mvnw spring-boot:run

# Terminal 3: login y llamadas
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin12345"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl localhost:8080/api/medicamentos -H "Authorization: Bearer $TOKEN"   # 200
curl localhost:8081/api/medicamentos                                     # 401: falta X-Api-Key
```

## Usuarios, roles y autenticación

Modelo de usuarios (`Rol`: `ADMIN`, `DOCTOR`, `ENFERMERO`; entidad `Usuario`,
repositorio en memoria, servicio con reglas de negocio) y creación de un
administrador inicial al arrancar. La autenticación es **stateless con JWT**:
todos los endpoints de `/api/**`, salvo el login, exigen un token bearer
válido. Aún **no hay autorización por rol ni endpoints de administración de
usuarios**: cualquier usuario autenticado (o el admin) puede usar los
endpoints, y los usuarios se crean vía `UsuarioService` (no por HTTP).

### Autenticación (JWT)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Público. `{ "username", "password" }` → JWT bearer |
| GET | `/api/auth/me` | Autenticado. Devuelve `{ idUsuario, username, rol, idTrabajador }` |

Login correcto:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu-contrasena"}'
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.…",
  "tipo": "Bearer",
  "expiraEnSegundos": 1800,
  "rol": "ADMIN"
}
```

El resto de endpoints se llaman con el token en `Authorization`:

```bash
curl http://localhost:8080/api/pacientes -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.…"
```

- El 401 del login es siempre `401 "Credenciales inválidas."`, indistinguible
  cuando el usuario no existe, la contraseña es incorrecta o el usuario está
  desactivado (las tres causas se evalúan con el mismo coste de BCrypt).
- El 401 de una petición sin token válido (ausente, malformado, expirado, con
  firma inválida o de un usuario desactivado) responde el cuerpo de error
  uniforme junto con `WWW-Authenticate: Bearer`; nunca se exponen detalles
  internos del token. El 403 responde `"No tiene permisos para realizar esta
  operación."`.
- El token (HS256) contiene `sub`, `username`, `rol` e `idTrabajador` (si
  aplica), pero la autorización **se relee del repositorio en cada petición**:
  si el usuario se desactiva (por ejemplo, al eliminar su trabajador), su token
  deja de valer de inmediato.

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

### Clave de firma JWT

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `JWT_SECRET` | `app.jwt.secret` | *(sin valor en el repo)* | Clave simétrica de firma HS256, mín. **32 bytes** codificados en UTF-8. |
| — | `app.jwt.emisor` | `pachoclosystem` | `iss` esperado en los tokens. |
| — | `app.jwt.expiracion` | `30m` | Duración de validez del token (`PT30M`, `30m`, `1800s`, …). |

- Si `JWT_SECRET` **no está definida**, se genera una clave aleatoria de 32
  bytes con `SecureRandom` solo para desarrollo y se escribe **un único WARN**
  en el log (sin el valor). Los tokens firmados así **no sobreviven a un
  reinicio**.
- Si `JWT_SECRET` tiene menos de 32 bytes, la aplicación **no arranca** y
  muestra un mensaje claro con la política; el valor nunca aparece en el log.

> El secreto y las contraseñas nunca se escriben en el log, en los errores ni en
> los `toString()`. Los tokens se validan contra el repositorio en cada petición:
> no hay sesiones en servidor ni tokens de refresco.

### Advertencia

> Los usuarios se guardan **en memoria**: se pierden al reiniciar la aplicación.
> Si no se define `ADMIN_PASSWORD`, la contraseña aleatoria **queda registrada en
> el log**; esa contraseña solo debe usarse en **desarrollo** y el log no debe
> compartirse. Para cualquier otro entorno, define `ADMIN_PASSWORD` y ten en
> cuenta que, aun así, los datos no sobreviven a un reinicio.
