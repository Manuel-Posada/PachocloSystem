package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import com.pachoclosystem.pachoclosystem.dto.MovimientoStockRequest;
import com.pachoclosystem.pachoclosystem.service.InventarioMedicamentosService;
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

/**
 * Inventario de medicamentos a través del servicio principal (requiere token,
 * como el resto de {@code /api/**}). Cada operación se reenvía a MedicamentosService.
 */
@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {

    private final InventarioMedicamentosService servicio;

    public MedicamentoController(InventarioMedicamentosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<MedicamentoResponse> listar(@RequestParam(required = false) String q) {
        return servicio.listar(q);
    }

    @GetMapping("/{id}")
    public MedicamentoResponse obtener(@PathVariable String id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<MedicamentoResponse> registrar(@RequestBody MedicamentoRequest request) {
        MedicamentoResponse medicamento = servicio.registrar(request);
        return ResponseEntity.created(URI.create("/api/medicamentos/" + medicamento.idMedicamento()))
                .body(medicamento);
    }

    @PutMapping("/{id}")
    public MedicamentoResponse editar(@PathVariable String id,
                                      @RequestBody ActualizarMedicamentoRequest request) {
        return servicio.editar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entradas")
    public MedicamentoResponse registrarEntrada(@PathVariable String id,
                                                @Valid @RequestBody MovimientoStockRequest request) {
        return servicio.registrarEntrada(id, request.cantidad());
    }

    @PostMapping("/{id}/salidas")
    public MedicamentoResponse registrarSalida(@PathVariable String id,
                                               @Valid @RequestBody MovimientoStockRequest request) {
        return servicio.registrarSalida(id, request.cantidad());
    }

    @GetMapping("/stock-bajo")
    public List<MedicamentoResponse> listarStockBajo() {
        return servicio.listarStockBajo();
    }

    @GetMapping("/por-vencer")
    public List<MedicamentoResponse> listarPorVencer(@RequestParam(required = false) Integer dias) {
        return servicio.listarPorVencer(dias);
    }

    @GetMapping("/vencidos")
    public List<MedicamentoResponse> listarVencidos() {
        return servicio.listarVencidos();
    }
}
