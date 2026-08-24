package PachocloSystem.model;

public class Doctor extends TrabajadorHospital {
    private String especialidad;

    
    public Doctor(String idTrabajador, String nombreCompleto, String especialidad) {
        super(idTrabajador, nombreCompleto);
        this.especialidad = especialidad;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    @Override
    public String obtenerPerfil() {
        return "Doctor: " + nombreCompleto + " | Especialidad: " + especialidad;
    }

    

}

