package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.dto.RegistroRequest;
import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.service.HistorialClinicoService;
import com.pachoclosystem.pachoclosystem.service.HistorialIdempotenteService;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HistorialClinicoController {

    /** Clave opcional para que repetir la creación de un registro no cree otro. */
    public static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";
    /** En la respuesta: {@code true} si es la repetición de un registro ya creado con esa clave. */
    public static final String CABECERA_REPETIDA = "Idempotency-Replayed";

    private final HistorialClinicoService servicio;
    private final HistorialIdempotenteService historialIdempotente;
    private final UsuarioService usuarioService;

    public HistorialClinicoController(HistorialClinicoService servicio,
                                      HistorialIdempotenteService historialIdempotente,
                                      UsuarioService usuarioService) {
        this.servicio = servicio;
        this.historialIdempotente = historialIdempotente;
        this.usuarioService = usuarioService;
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

    /**
     * Crea un registro firmado por el usuario autenticado. El usuario se relee
     * del repositorio por su username (el nombre de la autenticación), nunca de
     * los claims del token.
     *
     * <p>Sin {@code Idempotency-Key}, como siempre. Con ella, repetir la petición
     * con la misma clave devuelve el mismo registro (con
     * {@code Idempotency-Replayed: true}) sin crear otro ni volver a descontar
     * stock; ver {@link HistorialIdempotenteService}.</p>
     */
    @PostMapping("/api/pacientes/{id}/historial")
    public ResponseEntity<RegistroResponse> agregarRegistro(
            @PathVariable String id,
            @Valid @RequestBody RegistroRequest request,
            @RequestHeader(name = CABECERA_IDEMPOTENCIA, required = false) String clave,
            Authentication autenticacion) {
        Usuario usuario = usuarioService.buscarPorUsername(autenticacion.getName());
        if (clave == null) {
            RegistroResponse creado = servicio.agregarRegistroComo(
                    usuario, id, request.idAutor(), request.tipo(), request.contenido(),
                    request.signosVitales(), request.idMedicamento(), request.cantidad());
            return ResponseEntity.status(201).body(creado);
        }
        HistorialIdempotenteService.Resultado resultado =
                historialIdempotente.agregarRegistro(usuario, id, request, clave);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(201);
        if (resultado.repetido()) {
            respuesta.header(CABECERA_REPETIDA, "true");
        }
        return respuesta.body(resultado.registro());
    }
}
