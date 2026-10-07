/**
 * Configuración de compilación. `apiBaseUrl` vacía: la API está en el mismo
 * origen (proxy de `ng serve` en desarrollo). El build de Vercel sustituye este
 * archivo por `environment.vercel.ts`, que genera `scripts/configurar-api.mjs`
 * con la URL de la API (`API_BASE_URL`).
 */
export const environment = {
  apiBaseUrl: '',
};
