package com.pachoclosystem.demo.service;

import com.pachoclosystem.demo.dto.RegistroResponse;
import com.pachoclosystem.demo.dto.SignosVitalesRequest;
import com.pachoclosystem.demo.exception.NotFoundException;
import com.pachoclosystem.demo.exception.SolicitudInvalidaException;
import com.pachoclosystem.demo.model.Paciente;
import com.pachoclosystem.demo.model.RegistroClinico;
import com.pachoclosystem.demo.model.TipoRegistro;
import com.pachoclosystem.demo.model.TrabajadorHospital;
import com.pachoclosystem.demo.repository.IPacienteRepository;
import com.pachoclosystem.demo.repository.ITrabajadoresRepository;
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

    public RegistroResponse agregarRegistroPaciente(String idPaciente, String idAutor, TipoRegistro tipo,
                                                    String contenido, SignosVitalesRequest signos) {
        String contenidoFinal = construirContenido(tipo, contenido, signos);

        Paciente paciente = repositorioPacientes.buscarPorId(idPaciente);
        if (paciente == null) {
            throw new NotFoundException("No se encontró el paciente " + idPaciente + ".");
        }
        TrabajadorHospital autor = repositorioTrabajadores.buscarPorId(idAutor);
        if (autor == null) {
            throw new NotFoundException("No se encontró un trabajador con el ID " + idAutor + ".");
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
            for (RegistroClinico registro : paciente.obtenerHistorial()) {
                resultado.add(RegistroResponse.from(paciente, registro));
            }
        }
        resultado.sort(Comparator.comparing(RegistroResponse::fecha));
        return aplicarFiltro(resultado, modo, texto);
    }

    /** Registros de un paciente; si hay texto, filtra por autor (como la vista de un paciente). */
    public List<RegistroResponse> obtenerRegistrosPorPaciente(String idPaciente, String texto) {
        Paciente paciente = repositorioPacientes.buscarPorId(idPaciente);
        if (paciente == null) {
            throw new NotFoundException("No se encontró el paciente " + idPaciente + ".");
        }
        List<RegistroResponse> registros = paciente.obtenerHistorial().stream()
                .map(r -> RegistroResponse.from(paciente, r))
                .toList();
        return aplicarFiltro(registros, "autor", texto);
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
