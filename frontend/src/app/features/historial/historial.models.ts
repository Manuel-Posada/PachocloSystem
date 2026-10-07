import { Trabajador } from '../trabajadores/trabajador.models';

export type TipoRegistro = 'DIAGNOSTICO' | 'EVOLUCION' | 'MEDICACION' | 'SIGNOS_VITALES';

export const TIPOS_REGISTRO: readonly TipoRegistro[] = [
  'DIAGNOSTICO',
  'EVOLUCION',
  'MEDICACION',
  'SIGNOS_VITALES',
];

export const ETIQUETAS_TIPO: Readonly<Record<TipoRegistro, string>> = {
  DIAGNOSTICO: 'Diagnóstico',
  EVOLUCION: 'Evolución',
  MEDICACION: 'Medicación',
  SIGNOS_VITALES: 'Signos vitales',
};

/** Filtro del historial general: en qué campos busca `q`. */
export type FiltroHistorial = 'todos' | 'paciente' | 'autor';

/**
 * `RegistroResponse`. `fecha` es un `LocalDateTime` sin zona
 * ("2026-10-06T20:21:00.915"): se muestra tal cual (shared/fechas.ts).
 * En SIGNOS_VITALES, `contenido` es el texto que arma el backend
 * ("Signos vitales - Temp: 36.5°C | FC: 80 lpm | ..."). `medicacion` solo
 * viene en registros de MEDICACION que descontaron stock.
 */
export interface Registro {
  idRegistro: string;
  idPaciente: string;
  nombrePaciente: string;
  fecha: string;
  tipo: TipoRegistro;
  autor: Trabajador;
  contenido: string;
  medicacion?: { idMedicamento: string; cantidad: number };
}

/** `SignosVitalesRequest`. */
export interface SignosVitales {
  temperatura: number;
  frecCardiaca: number;
  presionSistolica: number;
  presionDiastolica: number;
  frecRespiratoria: number;
  saturacion: number;
  observaciones: string | null;
}

/**
 * `RegistroRequest`. SIGNOS_VITALES lleva `signosVitales`; el resto, `contenido`.
 * Sin autor: el backend firma con el trabajador del usuario autenticado.
 * `idMedicamento` y `cantidad`, los dos o ninguno y solo en MEDICACION,
 * descuentan stock antes de guardar el registro.
 */
export interface RegistroRequest {
  tipo: TipoRegistro;
  contenido: string | null;
  signosVitales: SignosVitales | null;
  idMedicamento: string | null;
  cantidad: number | null;
}
