package com.pachoclosystem.demo.controller;

import com.pachoclosystem.demo.dto.RegistroRequest;
import com.pachoclosystem.demo.dto.RegistroResponse;
import com.pachoclosystem.demo.service.HistorialClinicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HistorialClinicoController {

    private final HistorialClinicoService servicio;

    public HistorialClinicoController(HistorialClinicoService servicio) {
        this.servicio = servicio;
    }

    /** Historial general; filtro: todos (por defecto) | paciente | autor. */
    @GetMapping("/api/historial")
    public List<RegistroResponse> listarTodos(@RequestParam(required = false) String filtro,
                                              @RequestParam(required = false) String q) {
        return servicio.obtenerTodosLosRegistros(filtro, q);
    }

    @GetMapping("/api/pacientes/{id}/historial")
    public List<RegistroResponse> listarPorPaciente(@PathVariable String id,
                                                    @RequestParam(required = false) String q) {
        return servicio.obtenerRegistrosPorPaciente(id, q);
    }

    @PostMapping("/api/pacientes/{id}/historial")
    public ResponseEntity<RegistroResponse> agregarRegistro(@PathVariable String id,
                                                            @Valid @RequestBody RegistroRequest request) {
        RegistroResponse creado = servicio.agregarRegistroPaciente(
                id, request.idAutor(), request.tipo(), request.contenido(), request.signosVitales());
        return ResponseEntity.status(201).body(creado);
    }
}
