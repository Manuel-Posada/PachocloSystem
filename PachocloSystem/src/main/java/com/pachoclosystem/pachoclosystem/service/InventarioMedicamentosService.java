package com.pachoclosystem.pachoclosystem.service;

import com.pachoclosystem.pachoclosystem.client.MedicamentosClient;
import com.pachoclosystem.pachoclosystem.dto.ActualizarMedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoRequest;
import com.pachoclosystem.pachoclosystem.dto.MedicamentoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Inventario de medicamentos. Los datos viven en MedicamentosService; este
 * servicio es la puerta de entrada protegida por JWT y delega en el cliente HTTP.
 */
@Service
public class InventarioMedicamentosService {

    private final MedicamentosClient cliente;

    public InventarioMedicamentosService(MedicamentosClient cliente) {
        this.cliente = cliente;
    }

    public List<MedicamentoResponse> listar(String q) {
        return cliente.listar(q);
    }

    public MedicamentoResponse obtener(String id) {
        return cliente.obtener(id);
    }

    public MedicamentoResponse registrar(MedicamentoRequest solicitud) {
        return cliente.registrar(solicitud);
    }

    public MedicamentoResponse editar(String id, ActualizarMedicamentoRequest solicitud) {
        return cliente.editar(id, solicitud);
    }

    public void eliminar(String id) {
        cliente.eliminar(id);
    }

    public MedicamentoResponse registrarEntrada(String id, int cantidad) {
        return cliente.registrarEntrada(id, cantidad);
    }

    public MedicamentoResponse registrarSalida(String id, int cantidad) {
        return cliente.registrarSalida(id, cantidad);
    }

    public List<MedicamentoResponse> listarStockBajo() {
        return cliente.listarStockBajo();
    }

    /** {@code dias} nulo = el valor por defecto de MedicamentosService (30). */
    public List<MedicamentoResponse> listarPorVencer(Integer dias) {
        return cliente.listarPorVencer(dias);
    }

    public List<MedicamentoResponse> listarVencidos() {
        return cliente.listarVencidos();
    }
}
