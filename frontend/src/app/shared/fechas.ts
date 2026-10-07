/*
 * Fechas sin zona del backend (`LocalDate` "2027-01-31", `LocalDateTime`
 * "2026-10-06T20:21:00.915"). Se tratan siempre
 * como texto: nunca se pasan por `Date`, que las interpreta como medianoche
 * UTC y en zonas como la de Colombia (UTC-5) las mostraría un día antes.
 */

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

/** "2027-01-31" → "31/01/2027". Un texto con otro formato se devuelve tal cual. */
export function formatearFecha(fecha: string): string {
  const partes = FECHA_ISO.exec(fecha);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : fecha;
}

const FECHA_HORA_ISO = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/;

/**
 * "2026-10-06T20:21:00.915" → "06/10/2026 20:21": la hora tal como llega, sin
 * convertir de zona. Un texto con otro formato se devuelve tal cual.
 */
export function formatearFechaHora(fechaHora: string): string {
  const p = FECHA_HORA_ISO.exec(fechaHora);
  return p ? `${p[3]}/${p[2]}/${p[1]} ${p[4]}:${p[5]}` : fechaHora;
}
