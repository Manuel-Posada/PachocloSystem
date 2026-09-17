package com.PachocloSystem.PachocloSystem.service;

import com.PachocloSystem.PachocloSystem.model.Paciente;
import com.PachocloSystem.PachocloSystem.model.RegistroClinico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class HistorialClinicoService {

    private final PacienteService pacienteService;

    public Paciente agregarRegistro(String idPaciente, RegistroClinico nuevoRegistro) {
        Paciente paciente = pacienteService.buscarPorId(idPaciente);
        
        if (paciente.getRegistros() == null) {
            paciente.setRegistros(new ArrayList<>());
        }
        
        paciente.getRegistros().add(nuevoRegistro);
        return paciente;
    }
}