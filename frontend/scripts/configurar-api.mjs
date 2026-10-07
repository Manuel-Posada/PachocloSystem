// Genera src/environments/environment.vercel.ts con la URL de la API para el build
// de Vercel (npm run build:vercel), a partir de la variable de entorno API_BASE_URL.
//
// API_BASE_URL es el origen de PachocloSystem, sin barra final ni ruta, en https
// (p. ej. https://pachoclosystem.up.railway.app). Se admite http solo para localhost,
// para probar el build en local. Si falta o no es válida, el build falla.
import { writeFileSync } from 'node:fs';

const DESTINO = new URL('../src/environments/environment.vercel.ts', import.meta.url);

function fallar(motivo) {
  console.error(`API_BASE_URL: ${motivo}`);
  console.error('Ejemplo: API_BASE_URL=https://pachoclosystem.up.railway.app');
  process.exit(1);
}

const cruda = (process.env.API_BASE_URL ?? '').trim();
if (!cruda) {
  fallar('es obligatoria para el build de Vercel (origen de la API de PachocloSystem).');
}

let url;
try {
  url = new URL(cruda);
} catch {
  fallar('no es una URL absoluta válida.');
}

const local = url.hostname === 'localhost' || url.hostname === '127.0.0.1';
if (url.protocol !== 'https:' && !(url.protocol === 'http:' && local)) {
  fallar('debe usar https (http solo se admite para localhost).');
}
if (
  url.username ||
  url.password ||
  url.search ||
  url.hash ||
  (url.pathname !== '/' && url.pathname !== '')
) {
  fallar('debe ser solo el origen (esquema, host y puerto), sin ruta, consulta ni credenciales.');
}
if (cruda.endsWith('/')) {
  fallar('no debe terminar en "/".');
}

writeFileSync(
  DESTINO,
  `// Generado por scripts/configurar-api.mjs (npm run build:vercel). No editar ni versionar.
export const environment = {
  apiBaseUrl: ${JSON.stringify(url.origin)},
};
`,
);
console.log(`API_BASE_URL configurada: ${url.origin}`);
