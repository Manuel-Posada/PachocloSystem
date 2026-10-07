package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.AutorRegistro;
import com.pachoclosystem.pachoclosystem.model.Doctor;
import com.pachoclosystem.pachoclosystem.model.Enfermero;
import com.pachoclosystem.pachoclosystem.model.NivelExperiencia;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;

public record TrabajadorResponse(String idTrabajador, String nombreCompleto, String rol,
                                 String especialidad, NivelExperiencia nivelExperiencia) {

    public static TrabajadorResponse from(TrabajadorHospital t) {
        if (t instanceof Doctor d) {
            return new TrabajadorResponse(d.getIdTrabajador(), d.getNombreCompleto(), "Doctor",
                    d.getEspecialidad(), null);
        }
        Enfermero e = (Enfermero) t;
        return new TrabajadorResponse(e.getIdTrabajador(), e.getNombreCompleto(), "Enfermero",
                null, e.getNivelExperiencia());
    }

    /** El autor de un registro clínico, con los datos que tenía al firmarlo. */
    public static TrabajadorResponse from(AutorRegistro a) {
        return new TrabajadorResponse(a.idTrabajador(), a.nombreCompleto(), a.rol(), a.especialidad(),
                a.nivelExperiencia());
    }
}
