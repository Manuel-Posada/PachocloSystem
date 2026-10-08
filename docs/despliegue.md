# Despliegue: Railway + Vercel

Cómo está desplegado el sistema y cómo repetirlo. Los backends y sus bases viven en **Railway**; el
frontend, en **Vercel**. Aquí solo aparecen nombres de variables y referencias: los valores
secretos se configuran en cada plataforma y nunca se guardan en el repositorio.

```
 Navegador ──► Vercel (frontend) ──https──► Railway: PachocloSystem ──red privada──► MedicamentosService
                                              │  dominio público              (sin dominio público)
                                              ▼                                      ▼
                                       postgres-pacientes                  postgres-medicamentos
```

| Pieza | Plataforma | URL |
|---|---|---|
| Frontend | Vercel | <https://pachoclo-system-frontend.vercel.app> |
| PachocloSystem | Railway (público) | <https://pachoclosystem-production.up.railway.app> |
| MedicamentosService | Railway (solo red privada) | `http://<RAILWAY_PRIVATE_DOMAIN>:8081` |

## Railway

Un proyecto con cuatro servicios: dos PostgreSQL (`postgres-pacientes` y `postgres-medicamentos`,
uno por servicio) y los dos backends, creados desde el repositorio de GitHub. Los backends se
construyen con **Railpack** (detecta el `pom.xml`); no hace falta Dockerfile.

| Ajuste | PachocloSystem | MedicamentosService |
|---|---|---|
| Root Directory | `/PachocloSystem` | `/MedicamentosService` |
| Dominio público | Sí, puerto `8080` | **No** |
| Healthcheck Path | `/actuator/health/readiness` | `/actuator/health/readiness` |
| Réplicas | **1** (límite de login e idempotencia en memoria) | 1 |

Las tablas las crea **Flyway** al arrancar cada servicio; no hay migraciones manuales.

### Variables de MedicamentosService

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `RAILPACK_JDK_VERSION` | `25` (Railpack usa 21 por defecto y el proyecto exige Java 25) |
| `PORT` | `8081` (fijo, para que `MEDICAMENTOS_URL` sea estable) |
| `MEDICAMENTOS_DB_URL` | `jdbc:postgresql://${{postgres-medicamentos.PGHOST}}:${{postgres-medicamentos.PGPORT}}/${{postgres-medicamentos.PGDATABASE}}` |
| `MEDICAMENTOS_DB_USER` | `${{postgres-medicamentos.PGUSER}}` |
| `MEDICAMENTOS_DB_PASSWORD` | `${{postgres-medicamentos.PGPASSWORD}}` |
| `MEDICAMENTOS_API_KEY` | 🔒 secreto, 32 caracteres o más, **igual** en los dos servicios |

### Variables de PachocloSystem

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `RAILPACK_JDK_VERSION` | `25` |
| `PORT` | `8080` |
| `PACHOCLOSYSTEM_DB_URL` | `jdbc:postgresql://${{postgres-pacientes.PGHOST}}:${{postgres-pacientes.PGPORT}}/${{postgres-pacientes.PGDATABASE}}` |
| `PACHOCLOSYSTEM_DB_USER` | `${{postgres-pacientes.PGUSER}}` |
| `PACHOCLOSYSTEM_DB_PASSWORD` | `${{postgres-pacientes.PGPASSWORD}}` |
| `MEDICAMENTOS_URL` | `http://${{MedicamentosService.RAILWAY_PRIVATE_DOMAIN}}:8081` |
| `CORS_ORIGENES` | `https://pachoclo-system-frontend.vercel.app,https://pachoclosystem-production.up.railway.app` |
| `APP_LOGIN_PROXIES_CONFIABLES` | `100.64.0.0/10,152.233.23.0/24` (ver abajo) |
| `MEDICAMENTOS_API_KEY` | 🔒 secreto, la misma que en MedicamentosService |
| `JWT_SECRET` | 🔒 secreto, 32 bytes o más |
| `ADMIN_PASSWORD` | 🔒 secreto, solo hace falta hasta que se crea el administrador |

Notas:

- La URL de la base se compone con las variables `PG*` de Railway: su `DATABASE_URL` empieza por
  `postgresql://` y Spring necesita una URL JDBC. Usuario y contraseña van aparte.
- `MEDICAMENTOS_URL` usa `http`: el tráfico va por la red privada de Railway, que ya está cifrada.
- `CORS_ORIGENES` no admite comodines: los despliegues de *preview* de Vercel compilan, pero el
  navegador bloquea sus peticiones a la API.

### Proxies confiables (límite de intentos de login)

Las peticiones llegan a PachocloSystem a través de dos saltos de Railway: un proxy de borde con IP
pública y uno interno de la red `100.64.0.0/10`. Ninguno tiene una IP fija. Sin
`APP_LOGIN_PROXIES_CONFIABLES`, el servicio veía como IP del cliente la de esos proxies (varias,
alternándose), y el límite de intentos por usuario e IP no bloqueaba de forma fiable.

Con los dos rangos configurados se lee `X-Forwarded-For` saltando ambos proxies y se obtiene la IP
real del cliente: 5 fallos seguidos de un usuario desde una IP dan `429` a partir del sexto, y el
log `Login bloqueado temporalmente … IP …` muestra la IP pública del cliente.

`152.233.23.0/24` se dedujo de los logs (proxies de borde `152.233.23.193` y `.194`, borde `mia1`).
Railway no publica una lista fija: si en ese log vuelven a aparecer IPs públicas que no son de
clientes, hay que añadir su rango.

## Vercel

Proyecto importado del mismo repositorio. `frontend/vercel.json` define la instalación, el build y
la salida, y reescribe cualquier ruta a `index.html` (SPA).

| Ajuste | Valor |
|---|---|
| Root Directory | `frontend` |
| Preset | Angular u Other (manda `vercel.json`, con `"framework": null`) |
| `API_BASE_URL` | `https://pachoclosystem-production.up.railway.app` (Production y Preview) |

`npm run build:vercel` falla si falta `API_BASE_URL` o si no es un origen https sin barra final.
Si el dominio del frontend cambia, hay que actualizar `CORS_ORIGENES` en PachocloSystem y
redesplegarlo.

## Orden para desplegar desde cero

1. Railway: crear los dos PostgreSQL.
2. MedicamentosService: Root Directory, sin dominio, healthcheck y variables. Desplegar y comprobar
   en el log que Flyway aplica sus migraciones.
3. PachocloSystem: Root Directory, **generar el dominio antes de las variables** (hace falta para
   `CORS_ORIGENES`), healthcheck y variables. Desplegar.
4. Vercel: importar el repositorio con Root Directory `frontend` y `API_BASE_URL`. Desplegar.
5. Añadir el dominio de Vercel a `CORS_ORIGENES` y redesplegar PachocloSystem.

## Verificación

| Comprobación | Esperado |
|---|---|
| `GET /actuator/health` de PachocloSystem | `200 {"status":"UP"}` |
| `POST /api/auth/login` con el admin | `200` con token |
| `GET /api/medicamentos` con el token | `200`: red privada y `X-Api-Key` correctas |
| Preflight `OPTIONS` desde el origen de Vercel | `200` con `Access-Control-Allow-Origin` |
| Preflight desde otro origen | `403` |
| 6 logins fallidos seguidos de un usuario | 5 × `401` y después `429` constante |
| Crear un dato, redesplegar y volver a leerlo | El dato sigue ahí |
| Recargar una ruta profunda del frontend (`/pacientes`) | Carga la aplicación, no 404 |
