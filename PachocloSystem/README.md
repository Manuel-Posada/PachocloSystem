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
├── config/       configuración (seguridad, JWT, PasswordEncoder y admin inicial)
├── security/     JWT, entry point 401/403 y login
└── exception/    errores y @RestControllerAdvice
```

## Endpoints

### Pacientes — `/api/pacientes`

| Método | Ruta | Descripción | Rol | Respuestas |
|---|---|---|---|---|
| GET | `/api/pacientes?q=` | Lista pacientes; `q` filtra por id o nombre | Cualquiera | 200 |
| GET | `/api/pacientes/{id}` | Obtiene un paciente | Cualquiera | 200 / 404 |
| POST | `/api/pacientes` | Registra un paciente `{nombre, edad, habitacion}` | DOCTOR | 201 / 400 / 403 |
| PUT | `/api/pacientes/{id}` | Edita nombre, edad y habitación | DOCTOR | 200 / 400 / 403 / 404 |
| PATCH | `/api/pacientes/{id}/habitacion` | Cambia solo la habitación `{habitacion}` | DOCTOR | 200 / 400 / 403 / 404 |
| DELETE | `/api/pacientes/{id}` | Baja lógica de un paciente (ver abajo) | ADMIN | 204 / 403 / 404 |

**Baja lógica de pacientes.** `DELETE /api/pacientes/{id}` marca al paciente
como inactivo en lugar de borrarlo: el objeto y su historial clínico se
conservan en memoria. A partir de ahí el paciente se comporta como
inexistente (404 en `GET`/`PUT`/`PATCH`/historial y en un segundo `DELETE`) y
no se reactiva. Las bajas dobles devuelven 404.

### Trabajadores — `/api/trabajadores`

| Método | Ruta | Descripción | Rol | Respuestas |
|---|---|---|---|---|
| GET | `/api/trabajadores?q=` | Lista trabajadores; `q` filtra por id o nombre | Cualquiera | 200 |
| GET | `/api/trabajadores/{id}` | Obtiene un trabajador | Cualquiera | 200 / 404 |
| POST | `/api/trabajadores` | Registra `{nombre, rol: "Doctor"\|"Enfermero", especialidad?, nivelExperiencia?}` | ADMIN | 201 / 400 / 403 |
| PUT | `/api/trabajadores/{id}` | Edita un trabajador (el rol no puede cambiar) | ADMIN | 200 / 400 / 403 / 404 |
| DELETE | `/api/trabajadores/{id}` | Elimina un trabajador | ADMIN | 204 / 403 / 404 |

Un Doctor requiere `especialidad`; un Enfermero requiere `nivelExperiencia` (`NOVATO`, `PRINCIPIANTE`, `AVANZADO`).

### Historial clínico

| Método | Ruta | Descripción | Rol | Respuestas |
|---|---|---|---|---|
| GET | `/api/historial?filtro=&q=` | Todos los registros ordenados por fecha. `filtro`: `todos` (defecto), `paciente` o `autor` | Cualquiera | 200 / 400 |
| GET | `/api/pacientes/{id}/historial?q=` | Registros de un paciente; `q` filtra por autor | Cualquiera | 200 / 404 |
| POST | `/api/pacientes/{id}/historial` | Agrega un registro (ver abajo) | DOCTOR / ENFERMERO | 201 / 400 / 403 / 404 |

Cuerpo de `POST /api/pacientes/{id}/historial`:

```json
{ "tipo": "DIAGNOSTICO", "contenido": "Hipertensión leve" }
```

**El autor nunca se envía en el cuerpo.** El `autor` del registro es el usuario
autenticado que hace la petición, cuyo `idTrabajador` se relee del repositorio
(bajo ninguna circunstancia se toman los claims del token). Una propiedad
`idAutor` en el cuerpo se ignora.

`tipo` puede ser `DIAGNOSTICO`, `EVOLUCION`, `MEDICACION` o `SIGNOS_VITALES`. Para `SIGNOS_VITALES` se envía `signosVitales` en lugar de `contenido`:

```json
{
  "tipo": "SIGNOS_VITALES",
  "signosVitales": {
    "temperatura": 36.5, "frecCardiaca": 80,
    "presionSistolica": 120, "presionDiastolica": 80,
    "frecRespiratoria": 16, "saturacion": 98, "observaciones": "estable"
  }
}
```

Regla por rol: un **DOCTOR** puede registrar cualquier tipo; un **ENFERMERO**
solo `SIGNOS_VITALES` (para el resto recibe 403 con el cuerpo de error
uniforme, y esa comprobación precede a la validación del contenido y a la
búsqueda del paciente); un **ADMIN** no puede registrar historial (403 de ruta).

## Errores

Los errores devuelven un cuerpo uniforme:

```json
{ "status": 400, "error": "Bad Request", "mensajes": ["La especialidad es obligatoria."] }
```

## Usuarios, roles y autenticación

Modelo de usuarios (`Rol`: `ADMIN`, `DOCTOR`, `ENFERMERO`; entidad `Usuario`,
repositorio en memoria, servicio con reglas de negocio) y creación de un
administrador inicial al arrancar. La autenticación es **stateless con JWT**:
todos los endpoints de `/api/**`, salvo el login, exigen un token bearer
válido y autorización por rol (ver la matriz más abajo). La gestión de
usuarios está expuesta por HTTP bajo `/api/usuarios` (solo ADMIN) y cada
usuario puede cambiar su propia contraseña en `POST /api/auth/password`; todas
las operaciones pasan por `UsuarioService`, que valida la política de
contraseña y el vínculo con trabajadores.

### Autorización por rol

Cada petición se autentica con el JWT y se autoriza según el rol del usuario.
Una operación no permitida responde **403** con el mismo cuerpo de error
uniforme (`"No tiene permisos para realizar esta operación."`); sin token válido
la respuesta es **401**.

| Operación | ADMIN | DOCTOR | ENFERMERO |
|---|:---:|:---:|:---:|
| `POST /api/auth/login` (público) | ✔ | ✔ | ✔ |
| `GET /api/auth/me` | ✔ | ✔ | ✔ |
| `POST /api/auth/password` (cambio propio) | ✔ | ✔ | ✔ |
| `GET` pacientes / trabajadores / historial | ✔ | ✔ | ✔ |
| `POST` / `PUT` / `PATCH` pacientes | ✘ | ✔ | ✘ |
| `DELETE` paciente (baja lógica) | ✔ | ✘ | ✘ |
| `POST` / `PUT` / `DELETE` trabajadores | ✔ | ✘ | ✘ |
| `POST` historial — `SIGNOS_VITALES` | ✘ | ✔ | ✔ |
| `POST` historial — otros tipos | ✘ | ✔ | ✘ |
| `GET` / `POST` / `PATCH` `/api/usuarios*` | ✔ | ✘ | ✘ |

La lectura (identidad, pacientes, historial y trabajadores) está disponible para
cualquier usuario autenticado. Los 403 de ruta se producen **antes** de validar
el cuerpo o buscar el recurso; el 403 del ENFERMERO ante un tipo no permitido lo
emite el servicio, también antes de validar el contenido y de buscar al
paciente. Todas las respuestas de error usan el cuerpo uniforme, sin trazas ni
nombres de clases internas.

Quien **aún debe cambiar su contraseña** (alta por API, reset administrativo o
admin inicial con contraseña aleatoria) solo puede llamar a `GET /api/auth/me`
y a `POST /api/auth/password`: cualquier otra ruta de negocio responde **403**
con `"Debe cambiar su contraseña antes de continuar."` hasta completar el cambio.

### Autenticación (JWT)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Público. `{ "username", "password" }` → JWT bearer |
| GET | `/api/auth/me` | Autenticado. Devuelve `{ idUsuario, username, rol, idTrabajador, activo, debeCambiarPassword }` |
| POST | `/api/auth/password` | Autenticado. Cambia la contraseña del propio usuario: `{ "passwordActual", "passwordNueva" }` → `204`. El token usado queda revocado de inmediato y hay que volver a iniciar sesión. |

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
  "rol": "ADMIN",
  "debeCambiarPassword": false
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
  firma inválida, de un usuario desactivado o con la versión de token obsoleta)
  responde el cuerpo de error uniforme junto con `WWW-Authenticate: Bearer`;
  nunca se exponen detalles internos del token. El 403 responde `"No tiene
  permisos para realizar esta operación."`.
- El token (HS256) contiene `sub`, `username`, `rol`, `ver` e `idTrabajador`
  (si aplica), pero la autorización **se relee del repositorio en cada
  petición**: si el usuario se desactiva (por ejemplo, al eliminar su
  trabajador), su token deja de valer de inmediato.
- El claim `ver` es la **versión del token**: se incrementa con cada cambio de
  contraseña (propio o reset administrativo). El conversor de JWT comprueba en
  cada petición que `ver` coincida con la versión actual del usuario, de modo
  que un token emitido antes del cambio queda **revocado de inmediato** (401).

### Gestión de usuarios — `/api/usuarios` (solo ADMIN)

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/usuarios` | Lista todos los usuarios | 200 / 401 / 403 |
| GET | `/api/usuarios/{id}` | Obtiene un usuario | 200 / 401 / 403 / 404 |
| POST | `/api/usuarios` | Alta `{ username, password, rol, idTrabajador? }`; el usuario nace con el cambio de contraseña obligatorio | 201 / 400 / 401 / 403 / 404 |
| PATCH | `/api/usuarios/{id}/rol` | Cambia rol y trabajador vinculado `{ rol, idTrabajador? }` | 200 / 400 / 401 / 403 / 404 |
| PATCH | `/api/usuarios/{id}/estado` | Activa o desactiva `{ activo }` | 200 / 400 / 401 / 403 / 404 |
| POST | `/api/usuarios/{id}/password-reset` | Reset administrativo `{ password }`: nueva contraseña temporal (cambio obligatorio) y revocación de los tokens existentes | 200 / 400 / 401 / 403 / 404 |

`GET /api/auth/me` devuelve el mismo `UsuarioResponse` que la gestión de
usuarios, sin exponer la versión del token (el claim `ver` es un detalle
interno del JWT que solo evalúa el conversor de autenticación):
`{ idUsuario, username, rol, idTrabajador, activo, debeCambiarPassword }`.
Ninguna respuesta nunca incluye la contraseña ni su hash.

Reglas de negocio (aplicadas por `UsuarioService`):

- **Unicidad del username**: único sin distinguir mayúsculas, debe cumplir
  `^[a-z0-9._-]{3,30}$` y se normaliza a minúsculas.
- **Vínculo con trabajadores**: los administradores no se vinculan a ningún
  trabajador; los doctores y enfermeros deben vincularse a un trabajador
  existente de su tipo y que aún no tenga usuario. Cambiar de rol libera el
  trabajador anterior.
- **Alta y reset administrativo**: la cuenta nace con `debeCambiarPassword`
  `true`; el usuario entra con la contraseña temporal, se ve a sí mismo
  (`/api/auth/me`) y la cambia (`POST /api/auth/password`), pero ninguna otra
  ruta le responde hasta completar el cambio (403 con `"Debe cambiar su
  contraseña antes de continuar."`).
- **Cambio propio**: exige la contraseña actual, que la nueva sea distinta y
  cumpla la política; al completarse el token usado queda revocado (la versión
  `ver` se incrementa) y `debeCambiarPassword` pasa a `false`.
- **Estado**: un ADMIN no puede desactivarse a sí mismo y no se puede dejar al
  sistema sin ningún ADMIN activo. Desactivar una cuenta revoca sus tokens de
  inmediato; reactivar la cuenta de un trabajador ya eliminado se rechaza.

### Variables de entorno del administrador inicial

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `ADMIN_USERNAME` | `app.admin.username` | `admin` | Username del administrador inicial (se normaliza a minúsculas; debe cumplir `^[a-z0-9._-]{3,30}$`). |
| `ADMIN_PASSWORD` | `app.admin.password` | *(sin valor en el repo)* | Contraseña en claro del administrador. |

- Si `ADMIN_PASSWORD` **está definida**, se usa tal cual y **nunca se escribe en
  el log**. Si tiene menos de 10 caracteres, la aplicación **no arranca** y
  muestra un mensaje claro con la política. La cuenta nace operativa (sin cambio
  de contraseña obligatorio).
- Si `ADMIN_PASSWORD` **no está definida**, se genera una contraseña aleatoria
  de 20 caracteres (alfanumérico sin caracteres ambiguos) con `SecureRandom` y se
  escribe **una sola vez** en el log a nivel `WARN`. Como esa contraseña ha
  quedado escrita en el log, la cuenta nace **bloqueada**: el cambio de
  contraseña es obligatorio en el primer acceso antes de poder operar.

### Política de contraseña

- Entre **10 caracteres** y **72 bytes UTF-8**; no se exige ningún requisito de
  composición. El mensaje de error nunca incluye la contraseña.
- En el cambio de la contraseña propia, la nueva debe ser **diferente de la
  actual**.
- Solo se almacena el **hash BCrypt**: la contraseña en claro no se guarda ni
  aparece en `toString()`, en la serialización JSON ni en los logs.
- Un trabajador solo puede tener un usuario; los doctores y enfermeros deben
  estar vinculados a un trabajador existente de su tipo, y los administradores no
  se vinculan a ninguno.
- El alta por API y el reset administrativo crean contraseñas **temporales**: el
  usuario debe cambiarla en su primer acceso antes de poder operar (ver arriba).

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

## Límite de intentos de login

Protección contra fuerza bruta en `POST /api/auth/login`: un
`LimitadorIntentosLogin` en memoria mantiene contadores **independientes por
usuario y por IP**, ambos con la misma ventana de bloqueo temporal.

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `APP_LOGIN_MAX_INTENTOS` | `app.login.max-intentos` | `5` | Fallos consecutivos que bloquean la cuenta y la IP. |
| `APP_LOGIN_BLOQUEO_MINUTOS` | `app.login.bloqueo-minutos` | `15` | Duración de la ventana de bloqueo. |
| `APP_LOGIN_MAX_ENTRADAS` | `app.login.max-entradas` | `10000` | Tope de entradas del mapa (purga defensiva del límite de memoria). |

Comportamiento:

- Tras **5 fallos**, el 6º intento (contra ese usuario o desde esa IP) responde
  **`429 Too Many Requests`** con el cuerpo de error uniforme y la cabecera
  `Retry-After` en segundos. El bloqueo se comprueba **antes** de evaluar las
  credenciales, de modo que durante el bloqueo una contraseña correcta también
  responde 429.
- Un **acierto reinicia solo el contador del usuario**; el contador de la IP
  nunca se reinicia. La IP se bloquea con 5 fallos de usuarios distintos, y la
  **IP de origen es exclusivamente `getRemoteAddr()`**: la cabecera
  `X-Forwarded-For` se ignora (no se confía en ella para el bloqueo).
- Un **429 no registra un fallo**: los intentos durante el bloqueo no extienden
  la cuenta atrás, y la IP/usuario se desbloquean en solitario al agotarse la
  ventana. Un **400 por body inválido no cuenta ni comprueba el bloqueo**.
- El fallo se registra en las tres causas de 401 (usuario inexistente,
  contraseña incorrecta, usuario inactivo) y el 429 es **idéntico** exista o no
  el usuario (sin revelar su existencia). El username del log se normaliza a
  minúsculas, se truncan los caracteres de control y se limita a 30 caracteres;
  ninguna contraseña, hash ni token se escribe en el log.

## CORS

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `CORS_ORIGENES` | `app.cors.origenes` | *(vacío)* | Lista de orígenes permitidos separada por comas. |

Comportamiento:

- Con el valor por defecto (vacío) el CORS está **desactivado**: ninguna
  respuesta lleva cabeceras `Access-Control-Allow-*`.
- Cada origen debe ser una **URL absoluta `http(s)`, sin barra final y sin
  comodines**: un `*`, una URL con barra final o una URL no http(s) abortan el
  arranque con un mensaje claro.
- La configuración se aplica en la cadena de seguridad mediante
  `http.cors(...)`: los métodos permitidos son `GET`, `POST`, `PUT`, `PATCH`,
  `DELETE` y `OPTIONS`; las cabeceras permitidas `Authorization` y
  `Content-Type`; las expuestas `Retry-After`, `Location`. Nunca se usan
  credenciales (`allowCredentials=false`) y `maxAge` es de 1 hora.
- La **preflight OPTIONS de un origen permitido** se responde con 200 y sus
  cabeceras **sin exigir token**; la de un origen no permitido no recibe
  cabeceras CORS. Las respuestas (incluidos los 401/403/429) de un origen
  permitido llevan `Access-Control-Allow-Origin`.

### Advertencia

> Los usuarios se guardan **en memoria**: se pierden al reiniciar la aplicación.
> Si no se define `ADMIN_PASSWORD`, la contraseña aleatoria **queda registrada en
> el log**; esa contraseña solo debe usarse en **desarrollo** y el log no debe
> compartirse. Para cualquier otro entorno, define `ADMIN_PASSWORD` y ten en
> cuenta que, aun así, los datos no sobreviven a un reinicio.
