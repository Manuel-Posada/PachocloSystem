package com.pachoclosystem.pachoclosystem.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class RegistroClinico {

    private String idRegistro;
    private LocalDateTime fecha;
    private TrabajadorHospital autor;
    private TipoRegistro tipo;
    private String contenido;
    // Solo en registros de MEDICACION que descontaron stock; si no, null.
    private String idMedicamento;
    private Integer cantidad;

    public RegistroClinico(TipoRegistro tipo, String contenido, TrabajadorHospital autor) {
        this(tipo, contenido, autor, null, null);
    }

    public RegistroClinico(TipoRegistro tipo, String contenido, TrabajadorHospital autor,
                           String idMedicamento, Integer cantidad) {
        this.idRegistro = UUID.randomUUID().toString();
        this.fecha = LocalDateTime.now();
        this.tipo = tipo;
        this.contenido = contenido;
        this.autor = autor;
        this.idMedicamento = idMedicamento;
        this.cantidad = cantidad;
    }

    // Solo getters — el registro es inmutable una vez creado
    public String getIdRegistro() {
        return idRegistro;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TrabajadorHospital getAutor() {
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
