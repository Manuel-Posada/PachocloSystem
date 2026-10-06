package model;

public class Enfermero extends TrabajadorHospital{
    private NivelExperiencia nivelExperiencia;

    public Enfermero(String idTrabajador, String nombreCompleto, NivelExperiencia nivelExperiencia) {
        super(idTrabajador, nombreCompleto);
        this.nivelExperiencia = nivelExperiencia;
    }

    public NivelExperiencia getNivelExperiencia() {
        return nivelExperiencia;
    }

    public void setNivelExperiencia(NivelExperiencia nivelExperiencia) {
        this.nivelExperiencia = nivelExperiencia;
    }

    @Override
    public String obtenerPerfil() {
        return "Enfermero: " + nombreCompleto + " | Nivel: " + nivelExperiencia;
    }
    
}
