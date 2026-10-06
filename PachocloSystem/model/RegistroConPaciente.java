package model;

//DTO para hacer mas facil listar en una sola tabla los registros clinicos de todos los pacientes 
public class RegistroConPaciente {

    private final String idPaciente;
    private final String nombrePaciente;
    private final RegistroClinico registro;

    public RegistroConPaciente(String idPaciente, String nombrePaciente, RegistroClinico registro) {
        this.idPaciente = idPaciente;
        this.nombrePaciente = nombrePaciente;
        this.registro = registro;
    }

    public String getIdPaciente() {
        return idPaciente;
    }

    public String getNombrePaciente() {
        return nombrePaciente;
    }

    public RegistroClinico getRegistro() {
        return registro;
    }
}