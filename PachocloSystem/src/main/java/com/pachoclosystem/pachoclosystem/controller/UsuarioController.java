package com.pachoclosystem.pachoclosystem.controller;

import com.pachoclosystem.pachoclosystem.dto.CambiarEstadoRequest;
import com.pachoclosystem.pachoclosystem.dto.CambiarRolRequest;
import com.pachoclosystem.pachoclosystem.dto.CrearUsuarioRequest;
import com.pachoclosystem.pachoclosystem.dto.ResetPasswordRequest;
import com.pachoclosystem.pachoclosystem.dto.UsuarioResponse;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Gestión de usuarios, exclusiva de administradores: listado, consulta, alta
 * (con cambio de contraseña obligatorio), cambio de rol, cambio de estado y
 * reset administrativo de contraseña (también con cambio obligatorio).
 *
 * <p>El actor de las operaciones (necesario para impedir que un administrador
 * se desactive a sí mismo) se obtiene de la autenticación: nunca del cuerpo de
 * la petición.</p>
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable String id) {
        return UsuarioResponse.from(servicio.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        Usuario usuario = servicio.crearUsuario(request.username(), request.password(),
                rolDesde(request.rol()), request.idTrabajador(), true);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.getIdUsuario()))
                .body(UsuarioResponse.from(usuario));
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable String id,
                                      @Valid @RequestBody CambiarRolRequest request) {
        return UsuarioResponse.from(
                servicio.cambiarRol(id, rolDesde(request.rol()), request.idTrabajador()));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable String id,
                                         @Valid @RequestBody CambiarEstadoRequest request,
                                         Authentication autenticacion) {
        return UsuarioResponse.from(servicio.cambiarEstado(
                id, request.activo(), idDelActor(autenticacion)));
    }

    @PostMapping("/{id}/password-reset")
    public UsuarioResponse resetearPassword(@PathVariable String id,
                                            @Valid @RequestBody ResetPasswordRequest request) {
        return UsuarioResponse.from(servicio.resetearPassword(id, request.password()));
    }

    /**
     * Convierte el rol del cuerpo (ADMIN, DOCTOR o ENFERMERO, sin distinguir
     * mayúsculas). {@code null} o vacío llega al servicio, que responde con el
     * mismo mensaje que un valor inválido, sin inventar códigos de error.
     */
    private static Rol rolDesde(String rol) {
        if (rol == null || rol.isBlank()) {
            return null;
        }
        try {
            return Rol.valueOf(rol.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalido) {
            throw new SolicitudInvalidaException(
                    "Debe seleccionar un rol (ADMIN, DOCTOR o ENFERMERO).");
        }
    }

    private String idDelActor(Authentication autenticacion) {
        // El name de la autenticación es el username validado y activo (lo fija
        // el conversor de JWT releyendo el repositorio).
        return servicio.buscarPorUsername(autenticacion.getName()).getIdUsuario();
    }
}