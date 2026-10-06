package com.pachoclosystem.demo.controller;

import com.pachoclosystem.demo.dto.HabitacionRequest;
import com.pachoclosystem.demo.dto.PacienteRequest;
import com.pachoclosystem.demo.dto.PacienteResponse;
import com.pachoclosystem.demo.model.Paciente;
import com.pachoclosystem.demo.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService servicio;

    public PacienteController(PacienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PacienteResponse> listar(@RequestParam(required = false) String q) {
        return servicio.listarPacientes(q).stream().map(PacienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse obtener(@PathVariable String id) {
        return PacienteResponse.from(servicio.obtenerPaciente(id));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> registrar(@Valid @RequestBody PacienteRequest request) {
        Paciente paciente = servicio.registrarPaciente(request.nombre(), request.edad(), request.habitacion());
        return ResponseEntity.created(URI.create("/api/pacientes/" + paciente.getIdPaciente()))
                .body(PacienteResponse.from(paciente));
    }

    @PutMapping("/{id}")
    public PacienteResponse editar(@PathVariable String id, @Valid @RequestBody PacienteRequest request) {
        return PacienteResponse.from(
                servicio.editarPaciente(id, request.nombre(), request.edad(), request.habitacion()));
    }

    @PatchMapping("/{id}/habitacion")
    public PacienteResponse editarHabitacion(@PathVariable String id,
                                             @Valid @RequestBody HabitacionRequest request) {
        return PacienteResponse.from(servicio.editarHabitacion(id, request.habitacion()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }
}
