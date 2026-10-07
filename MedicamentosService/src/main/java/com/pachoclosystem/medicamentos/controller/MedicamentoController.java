package com.pachoclosystem.medicamentos.controller;

import com.pachoclosystem.medicamentos.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.medicamentos.dto.CrearMedicamentoRequest;
import com.pachoclosystem.medicamentos.dto.MedicamentoResponse;
import com.pachoclosystem.medicamentos.dto.MovimientoStockRequest;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.service.MedicamentoService;
import com.pachoclosystem.medicamentos.service.SalidasIdempotentesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {

    /** Clave opcional para que repetir una salida no vuelva a descontar. */
    public static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";
    /** En la respuesta: {@code true} si es la repetición de una salida ya hecha con esa clave. */
    public static final String CABECERA_REPETIDA = "Idempotency-Replayed";

    private final MedicamentoService servicio;
    private final SalidasIdempotentesService salidasIdempotentes;

    public MedicamentoController(MedicamentoService servicio,
                                 SalidasIdempotentesService salidasIdempotentes) {
        this.servicio = servicio;
        this.salidasIdempotentes = salidasIdempotentes;
    }

    @GetMapping
    public List<MedicamentoResponse> listar(@RequestParam(required = false) String q) {
        return respuestas(servicio.listarMedicamentos(q));
    }

    @GetMapping("/{id}")
    public MedicamentoResponse obtener(@PathVariable String id) {
        return respuesta(servicio.obtenerMedicamento(id));
    }

    @PostMapping
    public ResponseEntity<MedicamentoResponse> registrar(@Valid @RequestBody CrearMedicamentoRequest request) {
        Medicamento medicamento = servicio.registrarMedicamento(request.datos(), request.cantidadStock());
        return ResponseEntity.created(URI.create("/api/medicamentos/" + medicamento.getIdMedicamento()))
                .body(respuesta(medicamento));
    }

    @PutMapping("/{id}")
    public MedicamentoResponse editar(@PathVariable String id,
                                      @Valid @RequestBody ActualizarMedicamentoRequest request) {
        return respuesta(servicio.editarMedicamento(id, request.datos()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminarMedicamento(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entradas")
    public MedicamentoResponse registrarEntrada(@PathVariable String id,
                                                @Valid @RequestBody MovimientoStockRequest request) {
        return respuesta(servicio.registrarEntrada(id, request.cantidad()));
    }

    /**
     * Sin {@code Idempotency-Key}, una salida como siempre. Con ella, repetir la
     * petición con la misma clave y los mismos datos devuelve la misma respuesta
     * (con {@code Idempotency-Replayed: true}) sin volver a descontar.
     */
    @PostMapping("/{id}/salidas")
    public ResponseEntity<MedicamentoResponse> registrarSalida(
            @PathVariable String id,
            @Valid @RequestBody MovimientoStockRequest request,
            @RequestHeader(name = CABECERA_IDEMPOTENCIA, required = false) String clave) {
        if (clave == null) {
            return ResponseEntity.ok(respuesta(servicio.registrarSalida(id, request.cantidad())));
        }
        SalidasIdempotentesService.Resultado resultado =
                salidasIdempotentes.registrarSalida(id, request.cantidad(), clave);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (resultado.repetida()) {
            respuesta.header(CABECERA_REPETIDA, "true");
        }
        return respuesta.body(resultado.respuesta());
    }

    @GetMapping("/stock-bajo")
    public List<MedicamentoResponse> listarStockBajo() {
        return respuestas(servicio.listarStockBajo());
    }

    @GetMapping("/por-vencer")
    public List<MedicamentoResponse> listarPorVencer(@RequestParam(defaultValue = "30") int dias) {
        return respuestas(servicio.listarPorVencer(dias));
    }

    @GetMapping("/vencidos")
    public List<MedicamentoResponse> listarVencidos() {
        return respuestas(servicio.listarVencidos());
    }

    private MedicamentoResponse respuesta(Medicamento medicamento) {
        return MedicamentoResponse.from(medicamento, servicio.hoy());
    }

    private List<MedicamentoResponse> respuestas(List<Medicamento> medicamentos) {
        LocalDate hoy = servicio.hoy();
        return medicamentos.stream().map(m -> MedicamentoResponse.from(m, hoy)).toList();
    }
}
