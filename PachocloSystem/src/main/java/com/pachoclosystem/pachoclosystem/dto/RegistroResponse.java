package com.pachoclosystem.pachoclosystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;

import java.time.LocalDateTime;

/**
 * {@code medicacion} solo aparece en los registros de MEDICACION que descontaron
 * stock; en el resto no se serializa, así que esas respuestas no cambian.
 */
public record RegistroResponse(String idRegistro, String idPaciente, String nombrePaciente,
                               LocalDateTime fecha, TipoRegistro tipo, TrabajadorResponse autor,
                               String contenido,
                               @JsonInclude(JsonInclude.Include.NON_NULL) MedicacionResponse medicacion) {

    /** Medicamento y cantidad descontados del inventario por este registro. */
    public record MedicacionResponse(String idMedicamento, int cantidad) {
    }

    public static RegistroResponse from(Paciente p, RegistroClinico r) {
        return from(p.getIdPaciente(), p.getNombre(), r);
    }

    public static RegistroResponse from(String idPaciente, String nombrePaciente, RegistroClinico r) {
        MedicacionResponse medicacion = r.getIdMedicamento() == null
                ? null
                : new MedicacionResponse(r.getIdMedicamento(), r.getCantidad());
        return new RegistroResponse(r.getIdRegistro(), idPaciente, nombrePaciente,
                r.getFecha(), r.getTipo(), TrabajadorResponse.from(r.getAutor()), r.getContenido(),
                medicacion);
    }
}
