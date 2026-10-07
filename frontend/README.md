# Frontend de PachocloSystem

SPA en Angular 22 (standalone, zoneless, signals) con Angular Material.
Consume **solo** la API de PachocloSystem (`/api/**`, puerto 8080); nunca llama a MedicamentosService.
Plan completo y decisiones: [`docs/plan-frontend.md`](../docs/plan-frontend.md).

## Requisitos

- **Node** `^22.22.3`, `^24.15.0` o `>=26` (lo exige Angular 22).
- El backend PachocloSystem corriendo en `http://localhost:8080` (ver su README).
  MedicamentosService (8081) solo hace falta para el módulo de medicamentos.

## Cómo arrancar

```bash
npm install
npm start          # http://localhost:4200
```

En desarrollo, `proxy.conf.json` reenvía `/api` a `localhost:8080`, así que el navegador ve un
único origen y el backend **no necesita CORS**. En producción la SPA se sirve en el mismo origen
que la API.

Para entrar se usa el administrador inicial del backend (`ADMIN_USERNAME` / `ADMIN_PASSWORD`).
Desde **Usuarios** el admin crea los usuarios de doctores y enfermeros. Si se arranca con
`JWT_SECRET` fijo, la sesión sobrevive a reinicios del backend.

## Comandos

```bash
npm test                # tests unitarios (Vitest)
npm run lint            # ESLint (angular-eslint)
npm run format:check    # Prettier (npm run format para corregir)
npm run build           # build de producción en dist/frontend
```

## Estructura

```
src/app/
├── core/       sesión (AuthService, interceptores, guards), errores de la API (ApiError), roles y notificaciones
├── shared/     piezas reutilizables (validadores, errores de formulario, diálogo de confirmación)
├── layout/     marco de la app autenticada (barra superior y menú lateral)
└── features/   pantallas: login, 404 y módulos (pacientes, trabajadores, ...)
```

Convención: nombres de dominio en español y sufijos de Angular en inglés
(`LoginComponent`, `AuthService`, `authGuard`).

## Patrón de los módulos CRUD

Pacientes, trabajadores y medicamentos siguen el mismo patrón:

- **`<entidad>.service.ts`**: solo HTTP (listar con `?q=`, registrar, editar, eliminar). Sin estado.
- **`<entidad>s-lista`**: carga los datos con `crearListaRemota()` de `shared/listas.ts`, que da
  `elementos`, `cargando`, `errores`, la `consulta` de la última carga y `recargar()`. El buscador
  usa `textoBuscado()`, que espera a que se deje de escribir. La consulta puede ser cualquier tipo:
  medicamentos la usa para sus pestañas (`{ vista, texto | dias }`). Para el maquetado usa las
  clases globales `lista-*` de `styles.scss`.
- **Diálogos de formulario**: validan en local con `shared/validadores.ts` (mismas reglas y
  mensajes que el backend). Guardan ellos mismos, así un 400 se muestra dentro del formulario con
  `<app-errores-formulario>`, y se cierran con la entidad guardada.
- **Borrado**: `confirmar()` (`shared/confirmacion-dialogo`) y después `NotificacionService`.

## Roles

El backend usa dos formatos: `'ADMIN' | 'DOCTOR' | 'ENFERMERO'` para usuarios (login, `/me`, JWT) y
`'Doctor' | 'Enfermero'` para trabajadores. `core/roles.ts` es el único sitio que los convierte
(`rolDeUsuario`, `rolDeTrabajador`) y les pone etiqueta (`etiquetaRol`).

## Permisos por rol

`core/permisos.ts` es copia de la tabla "Permisos por rol" del README del backend
(`tienePermiso(rol, permiso)` y `PermisosService.puede(permiso)` para el usuario actual). Se usa
para ocultar lo que el rol no puede hacer:

- **Menú y rutas:** Trabajadores (`trabajadores.leer`) y Usuarios (`usuarios.gestionar`) solo
  aparecen a quien puede abrirlas, y sus rutas tienen `permisoGuard`, que lleva al inicio.
- **Acciones:** alta, edición y borrado de pacientes, escrituras de trabajadores y de medicamentos
  (el enfermero solo ve las salidas de stock) y el tipo `DIAGNOSTICO` del historial.

Quien decide es el backend: si algo se escapa, su 403 se muestra como cualquier otro error. Si la
tabla del backend cambia, hay que cambiar `core/permisos.ts` (su test la recorre fila a fila).

## Usuarios (solo ADMIN)

`/usuarios`: lista con búsqueda, alta (username, contraseña repetida, rol y, para doctor o
enfermero, un trabajador de su tipo sin usuario), restablecer contraseña y activar o desactivar.
`features/usuarios/politica.ts` copia del backend el formato del username y la política de
contraseña (de 10 caracteres a 72 bytes UTF-8 y distinta del username). Los 400 y 409 se muestran
dentro del formulario o del diálogo de confirmación, incluidos los de desactivarse a uno mismo o
al último administrador. Si el admin restablece su propia contraseña, el backend invalida su token:
se le avisa antes y, al guardar, vuelve al login con un mensaje que lo explica.

## Historial clínico

- `/historial` muestra todos los registros (solo consulta) y `/pacientes/:id/historial` los de un
  paciente, con acceso desde la lista de pacientes.
- Los registros se muestran **del más reciente al más antiguo** (el backend los devuelve al
  revés; `HistorialService` los ordena por fecha como texto).
- **Autor:** el backend firma cada registro con el trabajador del usuario autenticado; la petición
  no lleva `idAutor`. Un usuario sin trabajador vinculado (el admin) ve el historial en modo
  consulta, con un mensaje que explica por qué. El enfermero no ve el tipo `DIAGNOSTICO`.
- **Descuento de stock (MEDICACION):** el backend hace primero la salida en MedicamentosService y
  solo guarda el registro si sale bien. Si falla con un 4xx (stock insuficiente, vencido...), no
  se guarda nada y el formulario lo indica.
- **Altas idempotentes (`Idempotency-Key`):** cada alta lleva una clave `crypto.randomUUID()` en
  la cabecera `Idempotency-Key`, solo en `POST /api/pacientes/{id}/historial` (contrato en el
  README del backend, "Registros idempotentes"). Repetir con la misma clave y el mismo cuerpo
  devuelve el mismo registro sin crear otro ni descontar dos veces.
  - **Una clave por intento de registro:** nueva al abrir el diálogo, tras un alta correcta y tras
    un 409. Se reutiliza en los reintentos, y un doble clic mientras se envía no manda otra
    petición.
  - **201 con `Idempotency-Replayed: true`:** el registro ya existía; es un alta normal, sin aviso.
  - **Resultado incierto** (503 "No se pudo confirmar…", 502, 500 u otro 5xx, sin red o un error
    no HTTP): el alta pudo hacerse o no. Los campos quedan **bloqueados** y solo se ofrece
    **Reintentar** (misma clave y mismo cuerpo, sin riesgo) o **Cancelar**, que cierra el diálogo
    y avisa de revisar el historial del paciente y, si se pidió descuento, el stock. Mientras se
    envía o está bloqueado, el diálogo no se cierra con Esc ni clic fuera. Así no se puede
    reenviar con otros datos y otra clave tras un resultado incierto, lo que podría duplicar el
    registro o el descuento. Solo un alta correcta desbloquea, aunque el reintento falle de otra
    forma.
  - **409 (clave ya usada con otra petición):** se genera una clave nueva y se deja revisar y
    reenviar.
  - **Otros 4xx (400, 403, 404):** el backend no hizo nada ni consumió la clave. Se puede corregir
    y reenviar con la misma clave.
  - **Límites:** `crypto.randomUUID()` solo existe en contextos seguros (HTTPS o `localhost`;
    `ng serve` lo es). Las claves viven en el diálogo: si se recarga la página tras un resultado
    incierto, el siguiente intento lleva otra clave, así que conviene revisar el historial y el
    stock antes de repetirlo.
- **Signos vitales:** se envían estructurados, pero el backend los guarda como texto en `contenido`
  y así se muestran.

## Fechas

Las fechas sin zona del backend (`LocalDate`, p. ej. `fechaVencimiento: "2027-01-31"`, y
`LocalDateTime`, p. ej. la `fecha` de un registro) se manejan siempre como texto. La fecha se edita
con `<input type="date">`, que ya da `AAAA-MM-DD`, y se muestra con `formatearFecha()` o
`formatearFechaHora()` de `shared/fechas.ts`. Nunca se pasan por `Date`: lo interpretaría como medianoche
UTC y en UTC-5 mostraría el día anterior.

## Sesión y errores

- El token JWT vive en memoria y se copia en `sessionStorage`: aguanta una recarga y se pierde
  al cerrar la pestaña.
- Dura 30 minutos y no se renueva: se avisa 5 minutos antes y al expirar se vuelve al login,
  recordando la ruta en la que se estaba.
- Todo error HTTP llega a los componentes como `ApiError` (`status` y `mensajes`, los del
  `ErrorResponse` del backend). Sin ese cuerpo, por ejemplo con el backend apagado, el mensaje
  es "No se pudo conectar con el servidor". Un 401 fuera del login cierra la sesión.
- Si MedicamentosService no responde, el backend devuelve 503 (o 502). Medicamentos lo muestra
  como aviso dentro del módulo y el resto de la app sigue funcionando.
