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
├── core/       sesión (AuthService, interceptores, guards) y errores de la API (ApiError)
├── shared/     piezas reutilizables (validadores)
├── layout/     marco de la app autenticada (barra superior y menú lateral)
└── features/   pantallas: login, 404 y módulos (pacientes, trabajadores, ...)
```

Convención: nombres de dominio en español y sufijos de Angular en inglés
(`LoginComponent`, `AuthService`, `authGuard`).

## Sesión y errores

- El token JWT vive en memoria y se copia en `sessionStorage`: aguanta una recarga y se pierde
  al cerrar la pestaña.
- Dura 30 minutos y no se renueva: se avisa 5 minutos antes y al expirar se vuelve al login,
  recordando la ruta en la que se estaba.
- Todo error HTTP llega a los componentes como `ApiError` (`status` y `mensajes`, los del
  `ErrorResponse` del backend). Un 401 fuera del login cierra la sesión.
