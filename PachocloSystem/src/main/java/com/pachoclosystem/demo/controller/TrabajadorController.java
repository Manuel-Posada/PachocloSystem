package com.pachoclosystem.demo.controller;

import com.pachoclosystem.demo.dto.TrabajadorRequest;
import com.pachoclosystem.demo.dto.TrabajadorResponse;
import com.pachoclosystem.demo.model.TrabajadorHospital;
import com.pachoclosystem.demo.service.TrabajadorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/trabajadores")
public class TrabajadorController {

    private final TrabajadorService servicio;

    public TrabajadorController(TrabajadorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TrabajadorResponse> listar(@RequestParam(required = false) String q) {
        return servicio.listarTrabajadores(q).stream().map(TrabajadorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TrabajadorResponse obtener(@PathVariable String id) {
        return TrabajadorResponse.from(servicio.obtenerTrabajador(id));
    }

    @PostMapping
    public ResponseEntity<TrabajadorResponse> registrar(@Valid @RequestBody TrabajadorRequest request) {
        TrabajadorHospital t = servicio.registrarTrabajador(
                request.nombre(), request.rol(), request.especialidad(), request.nivelExperiencia());
        return ResponseEntity.created(URI.create("/api/trabajadores/" + t.getIdTrabajador()))
                .body(TrabajadorResponse.from(t));
    }

    @PutMapping("/{id}")
    public TrabajadorResponse editar(@PathVariable String id, @Valid @RequestBody TrabajadorRequest request) {
        return TrabajadorResponse.from(servicio.editarTrabajador(
                id, request.nombre(), request.rol(), request.especialidad(), request.nivelExperiencia()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminarTrabajador(id);
        return ResponseEntity.noContent().build();
    }
}
