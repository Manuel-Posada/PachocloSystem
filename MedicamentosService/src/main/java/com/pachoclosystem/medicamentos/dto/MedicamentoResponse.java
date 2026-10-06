package com.pachoclosystem.medicamentos.dto;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.model.Presentacion;

import java.time.LocalDate;

public record MedicamentoResponse(
        String idMedicamento,
        String nombre,
        String principioActivo,
        Presentacion presentacion,
        String concentracion,
        String laboratorio,
        String lote,
        int cantidadStock,
        int stockMinimo,
        LocalDate fechaVencimiento,
        String ubicacion,
        boolean stockBajo,
        boolean vencido) {

    public static MedicamentoResponse from(Medicamento m, LocalDate hoy) {
        DatosMedicamento d = m.getDatos();
        int stock = m.getCantidadStock();
        return new MedicamentoResponse(m.getIdMedicamento(), d.nombre(), d.principioActivo(),
                d.presentacion(), d.concentracion(), d.laboratorio(), d.lote(), stock,
                d.stockMinimo(), d.fechaVencimiento(), d.ubicacion(),
                stock <= d.stockMinimo(), d.fechaVencimiento().isBefore(hoy));
    }
}
