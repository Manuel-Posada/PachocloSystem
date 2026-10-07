# PachocloSystem

Sistema de gestión de un hospital: pacientes, trabajadores (doctores y enfermeros), historial
clínico, inventario de medicamentos y usuarios con roles. Nació como una aplicación Swing con
arquitectura MVC y hoy son tres procesos que se hablan por HTTP.

> Proyecto de desarrollo y aprendizaje: **todos los datos viven en memoria** y se pierden al
> reiniciar (ver [Limitaciones conocidas](#limitaciones-conocidas)).

## Arquitectura

```
 Navegador ──► Frontend Angular ──/api──► PachocloSystem ──X-Api-Key──► MedicamentosService
               (localhost:4200)           (localhost:8080)              (localhost:8081)
                                           JWT, roles, pacientes,        inventario y stock
                                           trabajadores, historial       de medicamentos
```

| Parte | Carpeta | Puerto | Qué hace |
|---|---|---|---|
| Frontend | [`frontend/`](frontend/) | 4200 | SPA Angular. Solo habla con PachocloSystem. |
| PachocloSystem | [`PachocloSystem/`](PachocloSystem/) | 8080 | API principal (Spring Boot): autenticación JWT, permisos por rol, pacientes, trabajadores, usuarios, historial clínico. Es la única puerta de entrada a los medicamentos. |
| MedicamentosService | [`MedicamentosService/`](MedicamentosService/) | 8081 | Microservicio de inventario (Spring Boot): stock, entradas y salidas, vencimientos. |

Cómo se comunican:

- **Navegador → PachocloSystem:** el frontend llama a rutas relativas `/api/**`. En desarrollo el
  proxy de Angular las reenvía al 8080, así que hay un único origen y no hace falta CORS. El
  usuario se autentica con un token JWT (`Authorization: Bearer`).
- **PachocloSystem → MedicamentosService:** HTTP directo con una clave compartida en la cabecera
  `X-Api-Key`. MedicamentosService no conoce a los usuarios, solo comprueba esa clave. Los dos
  servicios no comparten código.
- **El frontend nunca llama a MedicamentosService.** Si este está caído, PachocloSystem responde
  503 y solo el módulo de medicamentos muestra el aviso.

## Requisitos

- **Java 25** (`JAVA_HOME` apuntando a un JDK 25). No hace falta instalar Maven: cada servicio
  incluye su wrapper (`mvnw`).
- **Node** `^22.22.3`, `^24.15.0` o `>=26` (lo exige Angular 22), con npm.
- Git Bash o PowerShell en Windows; también vale cualquier shell POSIX.

## Cómo levantar el sistema

Hacen falta **tres terminales**, en este orden. Los valores de abajo son **solo de ejemplo para
desarrollo**: use los suyos y no los guarde en el repositorio.

| Variable | Dónde | Ejemplo | Notas |
|---|---|---|---|
| `MEDICAMENTOS_API_KEY` | los dos servicios | `clave-servicio-ejemplo` | Debe ser **igual** en ambos. |
| `ADMIN_PASSWORD` | PachocloSystem | `admin-ejemplo-2026` | Mínimo 10 caracteres. Si se omite, se genera una aleatoria y se imprime una vez en el log (WARN). |
| `JWT_SECRET` | PachocloSystem | `secreto-jwt-ejemplo-solo-desarrollo` | Mínimo 32 bytes. Si se omite, los tokens no sobreviven a un reinicio. |

### Git Bash

```bash
# Terminal 1: MedicamentosService (8081)
cd MedicamentosService
MEDICAMENTOS_API_KEY=clave-servicio-ejemplo ./mvnw spring-boot:run

# Terminal 2: PachocloSystem (8080)
cd PachocloSystem
MEDICAMENTOS_API_KEY=clave-servicio-ejemplo ADMIN_PASSWORD=admin-ejemplo-2026 \
  JWT_SECRET=secreto-jwt-ejemplo-solo-desarrollo ./mvnw spring-boot:run

# Terminal 3: frontend (4200)
cd frontend
npm install
npm start
```

### PowerShell

```powershell
# Terminal 1: MedicamentosService (8081)
cd MedicamentosService
$env:MEDICAMENTOS_API_KEY = "clave-servicio-ejemplo"
.\mvnw.cmd spring-boot:run

# Terminal 2: PachocloSystem (8080)
cd PachocloSystem
$env:MEDICAMENTOS_API_KEY = "clave-servicio-ejemplo"
$env:ADMIN_PASSWORD = "admin-ejemplo-2026"
$env:JWT_SECRET = "secreto-jwt-ejemplo-solo-desarrollo"
.\mvnw.cmd spring-boot:run

# Terminal 3: frontend (4200)
cd frontend
npm install
npm start
```

Abra <http://localhost:4200>.

### Usuario inicial

Al arrancar PachocloSystem se crea un administrador (solo en memoria):

- **Usuario:** `admin` (o el valor de `ADMIN_USERNAME`).
- **Contraseña:** la de `ADMIN_PASSWORD`. Con los ejemplos de arriba, `admin-ejemplo-2026`.

El administrador no tiene trabajador vinculado, así que puede consultar el historial pero no
crear registros. Doctores y enfermeros inician sesión con usuarios que el admin crea desde la
pantalla **Usuarios**.

## Recorrido corto

1. Entre como `admin`.
2. **Trabajadores:** registre un doctor (p. ej. nombre "Eva Mora", especialidad "Cardiología").
3. **Usuarios:** cree un usuario con rol `DOCTOR` vinculado a ese doctor (contraseña de 10
   caracteres o más).
4. **Pacientes:** registre un paciente.
5. **Medicamentos:** registre uno con stock inicial (esto habla con MedicamentosService).
6. Cierre sesión y entre con el usuario del doctor.
7. **Pacientes → Historial:** agregue un registro de tipo `MEDICACION` indicando el medicamento y
   una cantidad. El registro queda firmado por el doctor y el stock baja.
8. En **Medicamentos** compruebe el nuevo stock; en **Historial** vea el registro.
9. Opcional: detenga MedicamentosService y recargue Medicamentos para ver el aviso 503 sin que
   caiga el resto de la app.

## Tests

Cada parte se prueba por separado, desde su carpeta:

```bash
cd PachocloSystem && ./mvnw test          # API principal (en PowerShell: .\mvnw.cmd test)
cd MedicamentosService && ./mvnw test     # microservicio de medicamentos
cd frontend && npm test                   # unitarios (Vitest)
cd frontend && npm run build              # build de producción en frontend/dist
```

El frontend también tiene `npm run lint` y `npm run format:check`. No hace falta arrancar nada
para ejecutar los tests.

## Documentación por módulo

- [`PachocloSystem/README.md`](PachocloSystem/README.md): endpoints, permisos por rol, JWT, usuarios, historial idempotente e integración con medicamentos.
- [`MedicamentosService/README.md`](MedicamentosService/README.md): modelo, endpoints, stock, vencimientos e idempotencia de las salidas.
- [`frontend/README.md`](frontend/README.md): estructura, patrón de los módulos, roles, sesión y errores.
- [`docs/plan-frontend.md`](docs/plan-frontend.md): plan y decisiones del frontend.

## Limitaciones conocidas

Reúne lo que ya documentan los README de cada módulo (enlazados arriba). El detalle y el motivo
de cada punto están allí.

**Datos y sesión**

- **Todo en memoria.** Pacientes, trabajadores, usuarios, historial, medicamentos y claves de
  idempotencia se pierden al reiniciar cada servicio. Al arrancar solo se recrea el administrador
  inicial. Sin `ADMIN_PASSWORD` la contraseña es aleatoria y queda en el log; sin `JWT_SECRET` los
  tokens no sobreviven a un reinicio.
- **Sesión de 30 minutos sin renovación.** No hay refresh token: el frontend avisa 5 minutos antes
  y, al expirar, vuelve al login. El token se guarda en `sessionStorage` y se pierde al cerrar la
  pestaña.
- **Sin paginación:** las listas llegan completas.
- **Un usuario no puede cambiar su propia contraseña** (con la actual); solo el ADMIN la
  restablece, y eso invalida los tokens de ese usuario.

**Usuarios y trabajadores**

- Un trabajador solo puede tener **un usuario, aunque esté desactivado**: si su usuario se
  desactivó, no se le puede crear otro.
- Un doctor o enfermero desactivado solo se reactiva si su trabajador sigue existiendo, es de su
  tipo y sigue vinculado a él.

**Historial y stock de medicamentos**

- **Paciente borrado durante una salida de stock:** el paciente se lee antes de la salida y se
  guarda después. Si un ADMIN lo elimina mientras se espera la respuesta (con clave puede tardar
  hasta 2 × (`timeout-conexion` + `timeout-lectura`)), el paciente **se vuelve a crear** con su
  historial. Pasa con y sin `Idempotency-Key`.
- **Sin `Idempotency-Key`**, si la salida de stock se hace pero su respuesta no llega (timeout), el
  cliente recibe 503, el stock queda descontado sin registro y reintentar descontaría otra vez.
  Si el timeout salta con las cabeceras ya recibidas pero sin cuerpo, recibe **502** y no hay
  reintento. El frontend siempre envía la clave.
- **Las claves de idempotencia viven en memoria y caducan a las 24 h** (máx. 10 000 por servicio;
  al pasarse se descartan las caducadas y luego las más antiguas). "Reintentar sin riesgo de
  descontar dos veces" solo se cumple mientras MedicamentosService recuerde la clave: sin
  reiniciarse y dentro de ese plazo.
- **Las claves del frontend viven en el diálogo.** Si se recarga la página tras un resultado
  incierto, el siguiente intento usa otra clave: hay que revisar el historial y el stock antes de
  repetirlo.
- Los **signos vitales** se guardan como texto en `contenido`, no como datos estructurados.

**Despliegue**

- Frontend y API se asumen en el **mismo origen** (proxy en desarrollo); no hay CORS configurado.
- Sin `MEDICAMENTOS_API_KEY`, MedicamentosService no exige autenticación (solo desarrollo).

### Para una versión futura

- **Base de datos** en lugar de repositorios en memoria (los repositorios ya están detrás de
  interfaces) y, con ella, claves de idempotencia persistentes y datos que sobrevivan al reinicio.
- **Refresh token** para renovar la sesión sin volver a iniciar sesión.
- **Rol de farmacia** para gestionar el inventario sin ser ADMIN.
- **Tests E2E** con Playwright (previstos en el plan del frontend, fase F7).
- Cambio de contraseña propia, paginación y CORS si algún día el frontend se sirve desde otro origen.
