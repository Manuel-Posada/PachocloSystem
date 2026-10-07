/*
 * Fechas sin hora del backend (`LocalDate`, "2027-01-31"). Se tratan siempre
 * como texto: nunca se pasan por `Date`, que las interpreta como medianoche
 * UTC y en zonas como la de Colombia (UTC-5) las mostraría un día antes.
 */

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

/** "2027-01-31" → "31/01/2027". Un texto con otro formato se devuelve tal cual. */
export function formatearFecha(fecha: string): string {
  const partes = FECHA_ISO.exec(fecha);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : fecha;
}
