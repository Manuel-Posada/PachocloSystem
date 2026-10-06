package com.pachoclosystem.pachoclosystem.dto;

import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;

import java.time.LocalDateTime;

public record RegistroResponse(String idRegistro, String idPaciente, String nombrePaciente,
                               LocalDateTime fecha, TipoRegistro tipo, TrabajadorResponse autor,
                               String contenido) {

    public static RegistroResponse from(Paciente p, RegistroClinico r) {
        return new RegistroResponse(r.getIdRegistro(), p.getIdPaciente(), p.getNombre(),
                r.getFecha(), r.getTipo(), TrabajadorResponse.from(r.getAutor()), r.getContenido());
    }
}
