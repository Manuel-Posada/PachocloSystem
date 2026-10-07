# Plan del frontend Angular (`frontend/`)

Alcance: SPA en Angular que **solo** consume PachocloSystem (`:8080`, `/api/**`). Nunca llama a MedicamentosService (`:8081`).

## 1. Estado actual del backend

### Contratos
| Área | Endpoints | Notas para la UI |
|---|---|---|
| Auth | `POST /api/auth/login` → `{token, tipo:"Bearer", expiraEnSegundos, rol}`; `GET /api/auth/me` → `{idUsuario, username, rol, idTrabajador}` | Único endpoint público: login. El token dura 30 min y no hay refresh. |
| Pacientes | CRUD `/api/pacientes`, `PATCH /{id}/habitacion`, `?q=` | `{idPaciente, nombre, edad, habitacion}` |
| Trabajadores | CRUD `/api/trabajadores`, `?q=` | `rol` es `"Doctor"`/`"Enfermero"` (texto). Doctor → `especialidad`; Enfermero → `nivelExperiencia` (`NOVATO`/`PRINCIPIANTE`/`AVANZADO`). El rol no se puede editar. |
| Historial | `GET /api/historial?filtro=todos\|paciente\|autor&q=`, `GET/POST /api/pacientes/{id}/historial` | `tipo`: `DIAGNOSTICO`/`EVOLUCION`/`MEDICACION`/`SIGNOS_VITALES`. `MEDICACION` puede llevar `idMedicamento` y `cantidad` (los dos juntos) para descontar stock. |
| Medicamentos | CRUD `/api/medicamentos`, `POST /{id}/entradas\|salidas`, `GET stock-bajo`, `por-vencer?dias=1..365`, `vencidos` | Proxy de MedicamentosService. La respuesta trae `stockBajo` y `vencido` ya calculados. |

**Formato de error:** es uniforme en toda la API: `{status, error, mensajes: string[]}`. Eso incluye los 401 y 403 de seguridad y los errores que llegan de MedicamentosService. El frontend puede usar un solo manejador.

| Código | Cuándo | Acción en la UI |
|---|---|---|
| 400 | Validación (con varios mensajes) | Mostrarlos en el formulario |
| 401 | Login fallido, o token ausente, expirado o de un usuario desactivado | En login: mostrar el mensaje. En el resto: cerrar sesión e ir a `/login` |
| 403 | Hoy no se produce (no hay reglas por rol) | Mostrar "sin permisos" |
| 404 / 409 | No existe / duplicado (medicamentos) | Mensaje en pantalla |
| 502 / 503 | MedicamentosService responde mal o está caído | Aviso no bloqueante solo en el módulo Medicamentos; el resto de la app sigue funcionando |

**Fechas:** `fechaVencimiento` es `LocalDate` (`"2027-01-31"`). `fecha` del historial es `LocalDateTime` sin zona horaria: hay que mostrarla tal cual llega, sin convertir de zona.

### Huecos e inconsistencias que afectan al frontend
1. **No hay CORS.** Desde `localhost:4200` el navegador bloquea las llamadas. Se resuelve con un proxy de desarrollo (ver §2). Para producción hay que decidir (pregunta Q1).
2. **No hay endpoints de usuarios.** `UsuarioService.crearUsuario` existe, pero solo lo usa `AdminInicial`. Hoy **solo el admin puede iniciar sesión**: doctores y enfermeros no tienen forma de obtener usuario. Hace falta decidir si se agrega `POST/GET /api/usuarios` (pregunta Q2).
3. **No hay autorización por rol.** Todos los usuarios autenticados pueden hacer todo (`SecurityConfig`, `Rol.java` dice "Fase 2"). En la UI, ocultar opciones por rol sería solo estético mientras el backend no lo aplique (pregunta Q3).
4. **`idAutor` del historial lo escribe el cliente.** No sale del token, así que cualquiera puede firmar como otro trabajador. Además el admin no tiene `idTrabajador`. Opciones: la UI rellena `idAutor` con `me.idTrabajador`, o el backend lo toma del token (pregunta Q4).
5. **La validación de Medicamentos está solo en MedicamentosService.** El proxy no lleva `@Valid` en `POST`/`PUT`. Los 400 llegan bien, pero la UI debe replicar las reglas de `CrearMedicamentoRequest`: obligatorios, longitud máxima de 80 o 40, stock y mínimo entre 0 y 1.000.000, fecha obligatoria, `presentacion` dentro del enum.
6. **No hay endpoint para la lista de `Presentacion`, `TipoRegistro` ni `NivelExperiencia`.** Habrá que copiar los enums en el frontend.
7. **Los signos vitales se guardan como texto formateado** en `contenido`, no como datos estructurados. No se pueden graficar ni editar después sin cambiar el backend.
8. **No hay paginación** (todas las listas vienen completas) **ni refresh token**. Pasada la media hora, el usuario vuelve a iniciar sesión.
9. **Inconsistencia menor:** `rol` del trabajador es `"Doctor"` y `rol` del usuario es `"DOCTOR"`. Hay que mapear con cuidado.

**Validaciones a replicar en la UI** (los mensajes del backend se muestran igual):
- Nombre: `^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\s]{2,59}$`
- Edad: 0–120
- Habitación: 1–999
- `idAutor` e `idMedicamento`: `^[A-Za-z0-9-]{1,20}$`
- Cantidad: 1–1.000.000
- Contenido: al menos 5 caracteres y que no sean solo números
- Signos vitales:
  - Temperatura: 30–45
  - Frecuencia cardíaca: 20–250
  - Presión sistólica: 50–250
  - Presión diastólica: 30–150, y menor que la sistólica
  - Frecuencia respiratoria: 5–60
  - Saturación: 0–100
- `dias` (por vencer): 1–365

### Cambios mínimos de backend que propongo (cada uno con su propio PR, fuera de este plan)
- **B1.** Bean `CorsConfigurationSource` con orígenes por propiedad o variable de entorno (`app.cors.origenes`), solo si en producción el frontend se sirve desde otro origen.
- **B2.** `POST /api/usuarios` y `GET /api/usuarios`, solo para ADMIN.
- **B3.** Reglas por rol en `SecurityConfig` y, en el historial, tomar el autor del token.

## 2. Recomendaciones

| Tema | Opciones | Recomendación |
|---|---|---|
| **Estructura** | (a) por tipo (`components/`, `services/`) · (b) **por funcionalidad**: `core/` (auth, interceptores, guards, manejo de errores), `shared/` (UI reutilizable, validadores), `features/{pacientes,trabajadores,historial,medicamentos}` con rutas lazy | **(b)**, con componentes standalone y la última versión estable de Angular (signals, nuevo control flow). Escala mejor y cada fase toca solo su carpeta. |
| **Librería de UI** | **Angular Material**: oficial y accesible, sigue el ritmo de versiones de Angular; aspecto "Google". · PrimeNG: más componentes listos (tablas ricas), pero las versiones mayores rompen a menudo. · Tailwind solo: control total, pero todo se construye a mano. | **Angular Material**. Tablas, formularios, diálogos y snackbars cubren todo el alcance con mínimo riesgo de mantenimiento. |
| **Token** | (a) **en memoria + `sessionStorage`** · (b) `localStorage` · (c) cookie HttpOnly (exige cambiar el backend y reactivar CSRF) | **(a)**. El token vive en un servicio con signal y se copia en `sessionStorage` para sobrevivir a un F5; se borra al cerrar la pestaña. La expiración se lee de `expiraEnSegundos` y se cierra sesión automáticamente. Hay un interceptor que añade el `Bearer`. Ante un 401 fuera del login: logout y redirección guardando `returnUrl`. La cookie HttpOnly es más segura, pero va contra el diseño stateless actual. |
| **CORS vs proxy** | Proxy de desarrollo (`proxy.conf.json`: `/api` → `localhost:8080`) · CORS en el backend | **En desarrollo, proxy**: cero cambios en el backend y URLs relativas (`/api/...`). En producción, servir la SPA en el mismo origen, detrás de un reverse proxy o empaquetada en Spring. Así CORS no hace falta nunca. B1 solo aplica si se despliegan en dominios distintos. |
| **Estado** | (a) **servicios con signals** por funcionalidad · (b) NgRx Store · (c) NgRx SignalStore | **(a)**. El dominio es CRUD sin estado compartido complejo, así que NgRx sería ceremonia de sobra. Estado global solo para la sesión (`AuthStore`). Si crece, migrar a SignalStore. |
| **Tests** | Unitarios con el runner por defecto del CLI (Vitest) y `HttpTestingController` · E2E con Playwright contra el backend real o con mocks | **Unitarios** para servicios, interceptores, guards, validadores y el mapeo de errores, que es la lógica crítica. **Tests de componente** en los formularios. **Playwright** con pocos flujos de humo: login, alta de paciente, registro de medicación, servicio de medicamentos caído. E2E con la API mockeada en CI, y contra el backend real en local. |

## 3. Plan por fases

Cada fase se puede entregar y probar sola y se mergea con su propio PR.

| # | Fase | Contenido | Depende de | Cómo se prueba |
|---|---|---|---|---|
| **F0** | Base del proyecto | `ng new frontend` (standalone, SCSS, routing), Angular Material, ESLint y Prettier, `proxy.conf.json`, layout (toolbar y menú lateral), `environment`, scripts en el README | — | `npm start` levanta la app y `/api` se reenvía al 8080 |
| **F1** | Auth y núcleo | Login, `AuthStore` (signal + `sessionStorage`), interceptor de token, interceptor de errores (`ErrorResponse` → snackbar o formulario), `authGuard`, `/me` al arrancar, logout y expiración automática, página 404 | F0 | Unitarios del interceptor y el guard; login real con el admin; 401 al expirar |
| **F2** | Pacientes | Lista con búsqueda `q`, alta y edición (diálogo), cambio de habitación, borrado con confirmación, validadores compartidos | F1 | Unitarios del servicio y el formulario; e2e de alta |
| **F3** | Trabajadores | Lista y búsqueda, formulario condicional (especialidad o nivel según el rol), rol bloqueado al editar, aviso de que al borrar se desactiva su usuario | F1 | Ídem |
| **F4** | Medicamentos | Lista con badges de stock bajo y vencido, alta y edición (validaciones replicadas), entradas y salidas, pestañas de stock bajo / por vencer (`dias`) / vencidos, manejo de 409 y 503 | F1 | Unitarios; e2e con el servicio caído (503) |
| **F5** | Historial clínico | Historial general con filtro, historial por paciente (desde F2), formulario por tipo (contenido, signos vitales, o medicación con selector de medicamento y cantidad) | F2, F3, F4 (selector de medicamento) | Unitarios del formulario dinámico; e2e de registro de medicación que descuenta stock |
| **F6** | Roles y usuarios *(condicional)* | Menú y acciones según el rol; pantalla de usuarios si se hace B2 | B2/B3 en el backend | — |
| **F7** | Pulido y despliegue | Estados de carga y vacíos, accesibilidad, build de producción, estrategia de servido (mismo origen) | F2–F5 | Build de producción servido junto al backend |

Orden: F0 → F1 → (F2, F3 y F4 pueden ir en paralelo) → F5 → F6 → F7.

## 4. Preguntas para decidir antes de implementar

- **Q1. Despliegue:** ¿la SPA se servirá en el mismo origen que la API (reverse proxy, o empaquetada en Spring) o en otro dominio? (Decide si hace falta B1.)
- **Q2. Usuarios:** ¿agregamos endpoints de gestión de usuarios (B2) para que doctores y enfermeros puedan entrar, o el frontend se queda solo para el admin por ahora?
- **Q3. Roles:** ¿qué puede hacer cada rol (ADMIN, DOCTOR, ENFERMERO) en cada módulo? ¿Se aplica en el backend (B3) antes de la F6, o la UI solo oculta opciones?
- **Q4. Autor del historial:** ¿la UI rellena `idAutor` con el trabajador del usuario logueado (y qué hace el admin), o se cambia el backend para tomarlo del token?
- **Q5. Librería de UI:** ¿Angular Material te sirve, o hay preferencia o diseño corporativo (PrimeNG, Tailwind, mockups)?
- **Q6. Idioma y estilo de código:** ¿UI solo en español, sin i18n? ¿Nombres de código en español, como el backend?
- **Q7. Sesión:** ¿es aceptable volver a iniciar sesión cada 30 min (no hay refresh), o hay que añadir refresh token o alargar la expiración?
- **Q8. Tests E2E:** ¿Playwright entra en el alcance? ¿Hay CI (GitHub Actions) donde correr los tests del frontend?
- **Q9. Versiones:** ¿última estable de Angular con Node 24 (es el que tienes instalado), o alguna versión fija?

## 5. Decisiones tomadas

| # | Decisión |
|---|---|
| Q1 | Mismo origen. En desarrollo, proxy de Angular; sin CORS (no se hace B1). |
| Q2 | Se harán B2 (endpoints de usuarios) y B3 (reglas por rol), como PRs de backend separados antes de F6. Hasta entonces el frontend no los toca. **Estado final:** B2 y B3 se hicieron en el backend (`/api/usuarios`, solo ADMIN, y reglas por rol en `SecurityConfig`). F6 se implementó con la pantalla Usuarios (`/usuarios`): el admin crea usuarios de doctores y enfermeros, restablece contraseñas y los activa o desactiva. |
| Q3 | Los roles se aplicarán en el backend. De momento la UI no oculta nada por rol. **Estado final:** el backend aplica los roles (tabla "Permisos por rol" de su README) y, tras F6, la UI también oculta por rol lo que no se puede hacer (`core/permisos.ts`, que copia esa tabla). El backend sigue siendo quien decide: un 403 se muestra como cualquier otro error. Aquel "de momento no oculta nada" quedó superado. |
| Q4 | Hasta B3, la UI rellena `idAutor` con `me.idTrabajador`. El admin no puede crear registros de historial, solo consultarlos. **Estado final:** con B3 el backend firma cada registro con el trabajador del usuario autenticado y el frontend ya no envía `idAutor` (el backend lo sigue aceptando por compatibilidad). El admin, sin trabajador vinculado, solo consulta el historial, como se había previsto. |
| Q5 | Angular Material. |
| Q6 | UI solo en español, sin i18n. Nombres de dominio en español y sufijos de Angular en inglés (`PacienteService`, `LoginComponent`, `authGuard`). |
| Q7 | Sesión de 30 min sin refresh; se avisa al usuario 5 min antes de que expire. |
| Q8 | Playwright queda para F7, solo en local. |
| Q9 | Última estable de Angular (22.2.1), compatible con Node 24 (exige `^24.15.0`; instalado 24.20.0). |

**Notas de implementación**
- **Rama:** `feature-frontend-angular`, que parte de `integracion-medicamentos`. No se pudo usar `feature/frontend-angular` porque ya existe una rama `feature`, y Git no admite una rama y una "carpeta" de ramas con el mismo nombre.
- **Sin archivo `environment`:** se omitió el que F0 preveía. Todas las URLs son relativas (`/api/...`): en desarrollo las resuelve el proxy y en producción el mismo origen, así que no hay nada que configurar por entorno.
