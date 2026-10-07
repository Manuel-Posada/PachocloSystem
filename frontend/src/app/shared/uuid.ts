/** Lo que hace falta de `Crypto`: `randomUUID` no existe fuera de contextos seguros. */
export type FuenteAleatoria = Pick<Crypto, 'getRandomValues'> & Partial<Pick<Crypto, 'randomUUID'>>;

/**
 * UUID v4 aleatorio (36 caracteres: hex y guiones, válido como `Idempotency-Key`).
 * Usa `crypto.randomUUID()` si existe; en un contexto no seguro (HTTP fuera de
 * `localhost`) no existe, así que lo arma con `crypto.getRandomValues`, que sí.
 */
export function generarUuid(fuente: FuenteAleatoria = crypto): string {
  if (typeof fuente.randomUUID === 'function') {
    return fuente.randomUUID();
  }
  const bytes = fuente.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40; // versión 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80; // variante RFC 4122
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
