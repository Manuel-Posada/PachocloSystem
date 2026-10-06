package com.pachoclosystem.demo.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Paciente {

    private String idPaciente;
    private volatile String nombre;
    private volatile int edad;
    private volatile int habitacion;
    private List<RegistroClinico> registros;

    public Paciente(String idPaciente, String nombre, int edad, int habitacion) {
        this.idPaciente = idPaciente;
        this.nombre = nombre;
        this.edad = edad;
        this.habitacion = habitacion;
        this.registros = new CopyOnWriteArrayList<>();
    }

    public boolean estaDisponible() {
        return this.habitacion > 0;
    }

    public void actualizarDatos(int nuevaHabitacion) {
        this.habitacion = nuevaHabitacion;
    }

    //actualiza nombre, edad y habitación de una sola vez
    public void actualizarDatos(String nuevoNombre, int nuevaEdad, int nuevaHabitacion) {
        this.nombre = nuevoNombre;
        this.edad = nuevaEdad;
        this.habitacion = nuevaHabitacion;
    }

    public List<RegistroClinico> obtenerHistorial() {
        return this.registros;
    }

    public void agregarRegistro(RegistroClinico registro) {
        this.registros.add(registro);
    }

    public String getIdPaciente() {
        return idPaciente;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdad() {
        return edad;
    }

    public int getHabitacion() {
        return habitacion;
    }

    @Override
    public String toString() {
        return "ID: " + idPaciente + " | Nombre: " + nombre + " | Edad: " + edad
                + " | Habitación: " + habitacion + " | Registros: " + registros.size();
    }
}