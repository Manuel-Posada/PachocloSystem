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
├── core/       sesión (AuthService, interceptores, guards), errores de la API (ApiError) y notificaciones
├── shared/     piezas reutilizables (validadores, errores de formulario, diálogo de confirmación)
├── layout/     marco de la app autenticada (barra superior y menú lateral)
└── features/   pantallas: login, 404 y módulos (pacientes, trabajadores, ...)
```

Convención: nombres de dominio en español y sufijos de Angular en inglés
(`LoginComponent`, `AuthService`, `authGuard`).

## Patrón de los módulos CRUD

Pacientes (`features/pacientes/`) es la referencia para trabajadores y medicamentos:

- **`<entidad>.service.ts`**: solo HTTP (listar con `?q=`, registrar, editar, eliminar). Sin estado.
- **`<entidad>s-lista`**: guarda el estado en signals (`cargando`, `errores`, datos y filtro).
  Busca en el servidor al dejar de escribir, muestra los estados de carga, vacío y error (con
  reintento) y recarga tras cada cambio. Para el maquetado usa las clases globales `lista-*` de
  `styles.scss`.
- **Diálogos de formulario**: validan en local con `shared/validadores.ts` (mismas reglas y
  mensajes que el backend). Guardan ellos mismos, así un 400 se muestra dentro del formulario con
  `<app-errores-formulario>`, y se cierran con la entidad guardada.
- **Borrado**: `confirmar()` (`shared/confirmacion-dialogo`) y después `NotificacionService`.

## Sesión y errores

- El token JWT vive en memoria y se copia en `sessionStorage`: aguanta una recarga y se pierde
  al cerrar la pestaña.
- Dura 30 minutos y no se renueva: se avisa 5 minutos antes y al expirar se vuelve al login,
  recordando la ruta en la que se estaba.
- Todo error HTTP llega a los componentes como `ApiError` (`status` y `mensajes`, los del
  `ErrorResponse` del backend). Un 401 fuera del login cierra la sesión.
