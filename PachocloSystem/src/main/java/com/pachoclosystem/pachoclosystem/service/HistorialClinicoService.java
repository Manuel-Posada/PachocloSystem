package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.dto.RegistroResponse;
import com.pachoclosystem.pachoclosystem.dto.SignosVitalesRequest;
import com.pachoclosystem.pachoclosystem.exception.NotFoundException;
import com.pachoclosystem.pachoclosystem.exception.SolicitudInvalidaException;
import com.pachoclosystem.pachoclosystem.model.Paciente;
import com.pachoclosystem.pachoclosystem.model.RegistroClinico;
import com.pachoclosystem.pachoclosystem.model.Rol;
import com.pachoclosystem.pachoclosystem.model.TipoRegistro;
import com.pachoclosystem.pachoclosystem.model.TrabajadorHospital;
import com.pachoclosystem.pachoclosystem.model.Usuario;
import com.pachoclosystem.pachoclosystem.repository.IPacienteRepository;
import com.pachoclosystem.pachoclosystem.repository.ITrabajadoresRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class HistorialClinicoService {

    private static final Pattern PATRON_CONTIENE_TEXTO = Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");

    private final IPacienteRepository repositorioPacientes;
    private final ITrabajadoresRepository repositorioTrabajadores;

    public HistorialClinicoService(IPacienteRepository repositorioPacientes,
                                   ITrabajadoresRepository repositorioTrabajadores) {
        this.repositorioPacientes = repositorioPacientes;
        this.repositorioTrabajadores = repositorioTrabajadores;
    }

    /**
     * Agrega un registro clínico a un paciente. El autor es el usuario
     * autenticado (nunca un dato del cuerpo). Un ENFERMERO solo puede registrar
     * {@code SIGNOS_VITALES}; para el resto de tipos recibe 403, y esa
     * comprobación precede a la validación del contenido y a la búsqueda del
     * paciente.
     */
    public RegistroResponse agregarRegistroPaciente(String idPaciente, Usuario usuarioAutor, TipoRegistro tipo,
                                                    String contenido, SignosVitalesRequest signos) {
        if (usuarioAutor.getRol() == Rol.ENFERMERO && tipo != TipoRegistro.SIGNOS_VITALES) {
            throw new AccessDeniedException("No tiene permisos para realizar esta operación.");
        }

        String contenidoFinal = construirContenido(tipo, contenido, signos);

        Paciente paciente = buscarPacienteActivo(idPaciente);
        TrabajadorHospital autor = repositorioTrabajadores.buscarPorId(usuarioAutor.getIdTrabajador());
        if (autor == null) {
            throw new NotFoundException(
                    "No se encontró un trabajador con el ID " + usuarioAutor.getIdTrabajador() + ".");
        }

        RegistroClinico registro = new RegistroClinico(tipo, contenidoFinal, autor);
        paciente.agregarRegistro(registro);
        repositorioPacientes.guardarPaciente(paciente);
        return RegistroResponse.from(paciente, registro);
    }

    /** Todos los registros de todos los pacientes, ordenados por fecha. filtro: todos | paciente | autor. */
    public List<RegistroResponse> obtenerTodosLosRegistros(String filtro, String texto) {
        String modo = filtro == null || filtro.isBlank() ? "todos" : filtro.trim().toLowerCase(Locale.ROOT);
        if (!List.of("todos", "paciente", "autor").contains(modo)) {
            throw new SolicitudInvalidaException("El filtro debe ser todos, paciente o autor.");
        }

        List<RegistroResponse> resultado = new ArrayList<>();
        for (Paciente paciente : repositorioPacientes.obtenerTodos()) {
            // El historial global excluye los registros de pacientes dados de baja.
            if (!paciente.isActivo()) {
                continue;
            }
            for (RegistroClinico registro : paciente.obtenerHistorial()) {
                resultado.add(RegistroResponse.from(paciente, registro));
            }
        }
        resultado.sort(Comparator.comparing(RegistroResponse::fecha));
        return aplicarFiltro(resultado, modo, texto);
    }

    /** Registros de un paciente; si hay texto, filtra por autor (como la vista de un paciente). */
    public List<RegistroResponse> obtenerRegistrosPorPaciente(String idPaciente, String texto) {
        Paciente paciente = buscarPacienteActivo(idPaciente);
        List<RegistroResponse> registros = paciente.obtenerHistorial().stream()
                .map(r -> RegistroResponse.from(paciente, r))
                .toList();
        return aplicarFiltro(registros, "autor", texto);
    }

    /** Devuelve el paciente si existe y está activo; si no, 404 (un paciente dado de baja es inexistente). */
    private Paciente buscarPacienteActivo(String idPaciente) {
        Paciente paciente = repositorioPacientes.buscarPorId(idPaciente);
        if (paciente == null || !paciente.isActivo()) {
            throw new NotFoundException("No se encontró el paciente " + idPaciente + ".");
        }
        return paciente;
    }

    private List<RegistroResponse> aplicarFiltro(List<RegistroResponse> registros, String modo, String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (filtro.isEmpty()) {
            return registros;
        }
        return registros.stream().filter(rc -> {
            boolean coincidePaciente = rc.nombrePaciente().toLowerCase(Locale.ROOT).contains(filtro)
                    || rc.idPaciente().toLowerCase(Locale.ROOT).contains(filtro);
            boolean coincideAutor = rc.autor().nombreCompleto().toLowerCase(Locale.ROOT).contains(filtro)
                    || rc.autor().idTrabajador().toLowerCase(Locale.ROOT).contains(filtro);
            return switch (modo) {
                case "paciente" -> coincidePaciente;
                case "autor" -> coincideAutor;
                default -> coincidePaciente || coincideAutor;
            };
        }).toList();
    }

    private String construirContenido(TipoRegistro tipo, String contenido, SignosVitalesRequest signos) {
        List<String> errores = new ArrayList<>();

        if (tipo == TipoRegistro.SIGNOS_VITALES) {
            if (signos == null) {
                throw new SolicitudInvalidaException("Los signos vitales son obligatorios para este tipo de registro.");
            }
            if (signos.presionDiastolica() >= signos.presionSistolica()) {
                errores.add("La presión diastólica debe ser menor que la sistólica.");
            }
            String observaciones = signos.observaciones() == null ? "" : signos.observaciones().trim();
            if (!observaciones.isEmpty() && !PATRON_CONTIENE_TEXTO.matcher(observaciones).matches()) {
                errores.add("Las observaciones no pueden ser solo números.");
            }
            if (!errores.isEmpty()) {
                throw new SolicitudInvalidaException(errores);
            }
            String texto = String.format(Locale.US,
                    "Signos vitales - Temp: %.1f°C | FC: %d lpm | PA: %d/%d mmHg | FR: %d rpm | SpO2: %d%%",
                    signos.temperatura(), signos.frecCardiaca(), signos.presionSistolica(),
                    signos.presionDiastolica(), signos.frecRespiratoria(), signos.saturacion());
            return observaciones.isEmpty() ? texto : texto + " | Obs: " + observaciones;
        }

        String texto = contenido == null ? "" : contenido.trim();
        if (texto.isEmpty()) {
            errores.add("El contenido no puede estar vacío.");
        } else if (texto.length() < 5) {
            errores.add("El contenido es demasiado corto (mínimo 5 caracteres).");
        } else if (!PATRON_CONTIENE_TEXTO.matcher(texto).matches()) {
            errores.add("El contenido debe incluir texto descriptivo, no solo números.");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException(errores);
        }
        return texto;
    }
}
