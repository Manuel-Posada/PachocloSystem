package com.pachoclosystem.demo.dto;

import com.pachoclosystem.demo.model.Doctor;
import com.pachoclosystem.demo.model.Enfermero;
import com.pachoclosystem.demo.model.NivelExperiencia;
import com.pachoclosystem.demo.model.TrabajadorHospital;

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
}
