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

Para entrar se usa el administrador inicial del backend (`ADMIN_USERNAME` / `ADMIN_PASSWORD`). Si
se arranca con `JWT_SECRET` fijo, la sesión sobrevive a reinicios del backend.

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

## Historial clínico

- `/historial` muestra todos los registros (solo consulta) y `/pacientes/:id/historial` los de un
  paciente, con acceso desde la lista de pacientes.
- **Autor:** los registros se firman con `me.idTrabajador`. Regla provisional hasta que el backend
  tome el autor del token (B3). Un usuario sin trabajador vinculado (el admin) ve el historial en
  modo consulta, con un mensaje que explica por qué.
- **Descuento de stock (MEDICACION):** el backend hace primero la salida en MedicamentosService y
  solo guarda el registro si sale bien. Si falla, no se guarda nada y el formulario lo indica.
  Con 502/503 el formulario avisa de que la salida pudo registrarse igualmente, porque el backend
  no distingue un tiempo de espera de un servicio caído. Pendiente de corregir en el backend.
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
