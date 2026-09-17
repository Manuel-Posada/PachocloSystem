package com.PachocloSystem.PachocloSystem.controller;

import com.PachocloSystem.PachocloSystem.model.Paciente;
import com.PachocloSystem.PachocloSystem.model.RegistroClinico;
import com.PachocloSystem.PachocloSystem.service.HistorialClinicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/historial")
@RequiredArgsConstructor
public class ControladorHistorialClinico {

    private final HistorialClinicoService historialService;

    @PostMapping("/{idPaciente}")
    public ResponseEntity<Paciente> agregarRegistro(
            @PathVariable String idPaciente,
            @Valid @RequestBody RegistroClinico registro) {

        Paciente pacienteActualizado = historialService.agregarRegistro(idPaciente, registro);
        return new ResponseEntity<>(pacienteActualizado, HttpStatus.CREATED);
    }
}