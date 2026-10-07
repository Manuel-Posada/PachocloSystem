package com.pachoclosystem.pachoclosystem.security;

import com.pachoclosystem.pachoclosystem.dto.LoginResponse;
import com.pachoclosystem.pachoclosystem.exception.CredencialesInvalidasException;
import com.pachoclosystem.pachoclosystem.exception.DemasiadosIntentosException;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>Límite de intentos: antes de evaluar ninguna credencial se consulta el
 * {@link LimitadorIntentosLogin}; si el usuario o la IP están bloqueados se
 * lanza {@link DemasiadosIntentosException} ({@code 429} con {@code Retry-After})
 * sin registrar un nuevo fallo. Cada fallo de credencial (inexistente,
 * contraseña incorrecta o usuario inactivo) registra un fallo; el acierto
 * reinicia solo el contador del usuario.</p>
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final IUsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LimitadorIntentosLogin limitador;
    private final String hashFicticio;

    public AuthService(IUsuarioRepository repositorio, PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService, LimitadorIntentosLogin limitador) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limitador = limitador;
        // Hash BCrypt de una cadena que no corresponde a ningún usuario real:
        // sirve únicamente para igualar el tiempo de una comparación cuando el
        // username no existe. No es una credencial válida ni un secreto.
        this.hashFicticio = passwordEncoder.encode(
                "credenciales-ficticias-solo-para-igualar-tiempos-de-comparacion");
    }

    public LoginResponse iniciarSesion(String username, String passwordEnClaro, String ip) {
        // 1) Bloqueo por intentos fallidos ANTES de cualquier comprobación de
        // credenciales. El mensaje no revela si el usuario existe y el 429 no
        // registra un fallo nuevo (un bloqueado no extiende su bloqueo).
        long segundosBloqueo = limitador.estaBloqueado(username, ip);
        if (segundosBloqueo > 0) {
            LOG.warn("Login bloqueado temporalmente para el usuario '{}' ({}s restantes, IP {}).",
                    LimitadorIntentosLogin.sanearUsername(username), segundosBloqueo, ip);
            throw new DemasiadosIntentosException(segundosBloqueo);
        }

        Usuario usuario = repositorio.buscarPorUsername(username);
        if (usuario == null) {
            // Iguala el tiempo de respuesta al de un usuario existente con
            // contraseña incorrecta.
            passwordEncoder.matches(passwordEnClaro, hashFicticio);
            limitador.registrarFallo(username, ip);
            throw new CredencialesInvalidasException();
        }
        // Se evalúa la contraseña siempre (incluso si el usuario está inactivo)
        // para que las tres causas de 401 tarden lo mismo.
        boolean passwordCorrecta = passwordEncoder.matches(passwordEnClaro, usuario.getPasswordHash());
        if (!passwordCorrecta || !usuario.isActivo()) {
            limitador.registrarFallo(username, ip);
            throw new CredencialesInvalidasException();
        }
        limitador.registrarExito(username);
        String token = tokenService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", tokenService.expiraEnSegundos(),
                usuario.getRol().name(), usuario.isDebeCambiarPassword());
    }
}