package com.pachoclosystem.demo.dto;

import com.pachoclosystem.demo.model.Paciente;
import com.pachoclosystem.demo.model.RegistroClinico;
import com.pachoclosystem.demo.model.TipoRegistro;

import java.time.LocalDateTime;

public record RegistroResponse(String idRegistro, String idPaciente, String nombrePaciente,
                               LocalDateTime fecha, TipoRegistro tipo, TrabajadorResponse autor,
                               String contenido) {

    public static RegistroResponse from(Paciente p, RegistroClinico r) {
        return new RegistroResponse(r.getIdRegistro(), p.getIdPaciente(), p.getNombre(),
                r.getFecha(), r.getTipo(), TrabajadorResponse.from(r.getAutor()), r.getContenido());
    }
}
