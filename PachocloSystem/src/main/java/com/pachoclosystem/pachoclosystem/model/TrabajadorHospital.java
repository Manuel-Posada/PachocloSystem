package com.pachoclosystem.pachoclosystem.model;

public abstract class TrabajadorHospital {

    protected String idTrabajador;
    protected String nombreCompleto;

    public TrabajadorHospital(String idTrabajador, String nombreCompleto){
        this.idTrabajador = idTrabajador;
        this.nombreCompleto = nombreCompleto;   
    }

    public String getIdTrabajador() {
        return idTrabajador;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }
}