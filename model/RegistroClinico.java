package model;

import java.time.LocalDateTime;
import java.util.UUID;

public class RegistroClinico {

    private String idRegistro;
    private LocalDateTime fecha;
    private TrabajadorHospital autor;
    private TipoRegistro tipo;
    private String contenido;

    public RegistroClinico(TipoRegistro tipo, String contenido, TrabajadorHospital autor) {
        this.idRegistro = UUID.randomUUID().toString();
        this.fecha = LocalDateTime.now();
        this.tipo = tipo;
        this.contenido = contenido;
        this.autor = autor;
    }

    public String obtenerResumen() {
        return "[" + tipo + "] " + fecha + " - " + autor.getNombreCompleto() + ": " + contenido;
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
}