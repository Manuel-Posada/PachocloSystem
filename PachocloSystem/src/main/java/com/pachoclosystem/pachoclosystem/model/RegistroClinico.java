package com.pachoclosystem.pachoclosystem.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Registro del historial clínico. Inmutable una vez creado: guarda una copia del
 * autor ({@link AutorRegistro}) tal como era al firmarlo.
 *
 * <p>La fecha se trunca a microsegundos, la precisión de PostgreSQL: así el alta
 * y cualquier lectura posterior devuelven exactamente la misma fecha.</p>
 */
public class RegistroClinico {

    private final String idRegistro;
    private final LocalDateTime fecha;
    private final AutorRegistro autor;
    private final TipoRegistro tipo;
    private final String contenido;
    // Solo en registros de MEDICACION que descontaron stock; si no, null.
    private final String idMedicamento;
    private final Integer cantidad;

    public RegistroClinico(TipoRegistro tipo, String contenido, TrabajadorHospital autor) {
        this(tipo, contenido, autor, null, null);
    }

    public RegistroClinico(TipoRegistro tipo, String contenido, TrabajadorHospital autor,
                           String idMedicamento, Integer cantidad) {
        this(UUID.randomUUID().toString(), LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
                AutorRegistro.de(autor), tipo, contenido, idMedicamento, cantidad);
    }

    /** Reconstruye un registro ya guardado (lo usa el repositorio). */
    public RegistroClinico(String idRegistro, LocalDateTime fecha, AutorRegistro autor, TipoRegistro tipo,
                           String contenido, String idMedicamento, Integer cantidad) {
        this.idRegistro = idRegistro;
        this.fecha = fecha;
        this.autor = autor;
        this.tipo = tipo;
        this.contenido = contenido;
        this.idMedicamento = idMedicamento;
        this.cantidad = cantidad;
    }

    public String getIdRegistro() {
        return idRegistro;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public AutorRegistro getAutor() {
        return autor;
    }

    public TipoRegistro getTipo() {
        return tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public String getIdMedicamento() {
        return idMedicamento;
    }

    public Integer getCantidad() {
        return cantidad;
    }
}
