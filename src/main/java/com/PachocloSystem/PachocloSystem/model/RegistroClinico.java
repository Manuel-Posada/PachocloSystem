package com.PachocloSystem.PachocloSystem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroClinico {

    private String idRegistro;
    private String descripcion;
    private String fecha;
}