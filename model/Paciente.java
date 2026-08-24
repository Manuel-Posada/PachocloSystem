package model;

import java.util.ArrayList;
import java.util.List;

public class Paciente {

    private String idPaciente;
    private String nombre;
    private int edad;
    private int habitacion;
    private List<RegistroClinico> registros;

    public Paciente(String idPaciente, String nombre, int edad, int habitacion) {
        this.idPaciente = idPaciente;
        this.nombre = nombre;
        this.edad = edad;
        this.habitacion = habitacion;
        this.registros = new ArrayList<>();
    }

    public boolean estaDisponible() {
        return this.habitacion > 0;
    }

    public void actualizarDatos(int nuevaHabitacion) {
        this.habitacion = nuevaHabitacion;
    }

    public List<RegistroClinico> obtenerHistorial() {
        return this.registros;
    }

    public void agregarRegistro(RegistroClinico registro) {
        this.registros.add(registro);
    }

    // Getters básicos (no están en el UML, pero los necesitarás para
    // que el repositorio pueda guardar/consultar por id, nombre, etc.)
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
}
