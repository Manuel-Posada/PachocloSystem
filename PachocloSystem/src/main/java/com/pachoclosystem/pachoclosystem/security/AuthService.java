package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.LoginResponse;
import com.pachoclosystem.pachoclosystem.exception.CredencialesInvalidasException;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Login: valida credenciales contra el hash BCrypt del repositorio y emite el
 * token JWT.
 *
 * <p>Anti-enumeración de usuarios: las tres causas de rechazo (username
 * inexistente, contraseña incorrecta, usuario desactivado) responden el mismo
 * {@code 401} con el mismo mensaje y una duración equivalente del cálculo
 * BCrypt (contra un hash ficticio cuando el usuario no existe).</p>
 */
@Service
public class AuthService {

    private final IUsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final String hashFicticio;

    public AuthService(IUsuarioRepository repositorio, PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        // Hash BCrypt de una cadena que no corresponde a ningún usuario real:
        // sirve únicamente para igualar el tiempo de una comparación cuando el
        // username no existe. No es una credencial válida ni un secreto.
        this.hashFicticio = passwordEncoder.encode(
                "credenciales-ficticias-solo-para-igualar-tiempos-de-comparacion");
    }

    public LoginResponse iniciarSesion(String username, String passwordEnClaro) {
        Usuario usuario = repositorio.buscarPorUsername(username);
        if (usuario == null) {
            // Iguala el tiempo de respuesta al de un usuario existente con
            // contraseña incorrecta.
            passwordEncoder.matches(passwordEnClaro, hashFicticio);
            throw new CredencialesInvalidasException();
        }
        // Se evalúa la contraseña siempre (incluso si el usuario está inactivo)
        // para que las tres causas de 401 tarden lo mismo.
        // Una sola lectura de las credenciales: el token lleva la marca del hash
        // comprobado, así un restablecimiento simultáneo no deja pasar la anterior.
        Usuario.Credenciales credenciales = usuario.getCredenciales();
        boolean passwordCorrecta = passwordEncoder.matches(passwordEnClaro, credenciales.hash());
        if (!passwordCorrecta || !usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        String token = tokenService.generarToken(usuario, credenciales);
        return new LoginResponse(token, "Bearer", tokenService.expiraEnSegundos(),
                usuario.getRol().name());
    }
}