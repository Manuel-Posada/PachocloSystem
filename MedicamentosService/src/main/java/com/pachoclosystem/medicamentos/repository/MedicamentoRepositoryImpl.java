package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio de medicamentos en memoria basado en {@link ConcurrentHashMap}.
 *
 * <p>La unicidad de nombre + concentración + presentación + lote se resuelve con
 * {@code putIfAbsent} sobre un índice auxiliar, operación atómica en las
 * colecciones concurrentes. Los cambios de datos de un mismo medicamento se
 * serializan con su propio monitor, para que el índice nunca quede obsoleto.</p>
 */
@Repository
public class MedicamentoRepositoryImpl implements IMedicamentoRepository {

    private static final String PREFIJO_ID = "MED";

    /** ID de medicamento -> medicamento. */
    private final Map<String, Medicamento> medicamentos = new ConcurrentHashMap<>();
    /** Clave única normalizada -> ID de medicamento. */
    private final Map<String, String> indicePorClave = new ConcurrentHashMap<>();

    private final AtomicInteger contadorId = new AtomicInteger(1);

    @Override
    public String generarNuevoId() {
        return String.format("%s-%04d", PREFIJO_ID, contadorId.getAndIncrement());
    }

    @Override
    public boolean guardar(Medicamento medicamento) {
        if (medicamento == null) {
            return false;
        }
        String id = medicamento.getIdMedicamento();
        if (!reservarClave(medicamento.getDatos().claveUnica(), id)) {
            return false;
        }
        medicamentos.put(id, medicamento);
        return true;
    }

    @Override
    public boolean actualizarDatos(Medicamento medicamento, DatosMedicamento nuevosDatos) {
        String id = medicamento.getIdMedicamento();
        synchronized (medicamento) {
            if (medicamentos.get(id) != medicamento) {
                // Eliminado entre la búsqueda y la edición: no se reserva ninguna clave
                // para un id que ya no existe.
                medicamento.setDatos(nuevosDatos);
                return true;
            }
            String claveVieja = medicamento.getDatos().claveUnica();
            String claveNueva = nuevosDatos.claveUnica();
            if (!reservarClave(claveNueva, id)) {
                return false;
            }
            medicamento.setDatos(nuevosDatos);
            if (!claveNueva.equals(claveVieja)) {
                indicePorClave.remove(claveVieja, id);
            }
            return true;
        }
    }

    @Override
    public Medicamento buscarPorId(String id) {
        return id == null ? null : medicamentos.get(id);
    }

    @Override
    public List<Medicamento> listarTodos() {
        List<Medicamento> listados = new ArrayList<>(medicamentos.values());
        listados.sort(Comparator.comparing(Medicamento::getIdMedicamento));
        return listados;
    }

    @Override
    public boolean eliminar(String id) {
        Medicamento eliminado = id == null ? null : medicamentos.remove(id);
        if (eliminado == null) {
            return false;
        }
        synchronized (eliminado) {
            indicePorClave.remove(eliminado.getDatos().claveUnica(), id);
        }
        return true;
    }

    /** Reserva la clave para el id; es válida si estaba libre o ya era suya. */
    private boolean reservarClave(String clave, String id) {
        String duenoActual = indicePorClave.putIfAbsent(clave, id);
        return duenoActual == null || duenoActual.equals(id);
    }
}
