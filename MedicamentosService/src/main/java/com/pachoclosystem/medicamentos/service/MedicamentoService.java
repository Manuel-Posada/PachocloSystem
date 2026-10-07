package com.pachoclosystem.medicamentos.service;

import com.pachoclosystem.medicamentos.exception.ConflictoException;
import com.pachoclosystem.medicamentos.exception.NotFoundException;
import com.pachoclosystem.medicamentos.exception.SolicitudInvalidaException;
import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.repository.IMedicamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio de los medicamentos.
 *
 * <p>Las operaciones que cambian un medicamento existente (entradas, salidas y
 * edición) se ejecutan en una transacción y leen el medicamento con
 * {@link IMedicamentoRepository#buscarPorIdParaActualizar}: comprobar y escribir
 * ocurre bajo el mismo bloqueo de fila, así que dos salidas simultáneas nunca
 * dejan el stock en negativo ni se pierde una actualización.</p>
 */
@Service
public class MedicamentoService {

    public static final int DIAS_POR_VENCER_MIN = 1;
    public static final int DIAS_POR_VENCER_MAX = 365;

    private static final Comparator<Medicamento> POR_VENCIMIENTO =
            Comparator.comparing((Medicamento m) -> m.getDatos().fechaVencimiento())
                    .thenComparing(Medicamento::getIdMedicamento);

    private final IMedicamentoRepository repositorio;
    private final Clock reloj;

    public MedicamentoService(IMedicamentoRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public Medicamento registrarMedicamento(DatosMedicamento datos, int cantidadStock) {
        Medicamento medicamento = new Medicamento(repositorio.generarNuevoId(), recortar(datos), cantidadStock);
        if (!repositorio.guardar(medicamento)) {
            throw duplicado();
        }
        return medicamento;
    }

    /**
     * Edita los datos descriptivos; el stock no cambia. Si el medicamento se
     * elimina a la vez, la edición responde 404.
     */
    @Transactional
    public Medicamento editarMedicamento(String id, DatosMedicamento datos) {
        Medicamento medicamento = obtenerParaActualizar(id);
        switch (repositorio.actualizarDatos(medicamento, recortar(datos))) {
            case DUPLICADO -> throw duplicado();
            case NO_EXISTE -> throw noEncontrado(id);
            case ACTUALIZADO -> {
            }
        }
        return medicamento;
    }

    @Transactional
    public void eliminarMedicamento(String id) {
        if (!repositorio.eliminar(id)) {
            throw noEncontrado(id);
        }
    }

    public Medicamento obtenerMedicamento(String id) {
        Medicamento medicamento = repositorio.buscarPorId(id);
        if (medicamento == null) {
            throw noEncontrado(id);
        }
        return medicamento;
    }

    /** Lista medicamentos; si hay texto, filtra por id, nombre o principio activo. */
    public List<Medicamento> listarMedicamentos(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return repositorio.listarTodos().stream()
                .filter(m -> filtro.isEmpty()
                        || contiene(m.getIdMedicamento(), filtro)
                        || contiene(m.getDatos().nombre(), filtro)
                        || contiene(m.getDatos().principioActivo(), filtro))
                .toList();
    }

    @Transactional
    public Medicamento registrarEntrada(String id, int cantidad) {
        Medicamento medicamento = obtenerParaActualizar(id);
        medicamento.ingresarStock(cantidad);
        repositorio.actualizarStock(id, medicamento.getCantidadStock());
        return medicamento;
    }

    /**
     * Resta stock; falla si no alcanza o si el medicamento está vencido. Si ya
     * hay una transacción (salida con clave de idempotencia), se une a ella.
     */
    @Transactional
    public Medicamento registrarSalida(String id, int cantidad) {
        Medicamento medicamento = obtenerParaActualizar(id);
        if (medicamento.estaVencido(hoy())) {
            throw new SolicitudInvalidaException("No se puede dar salida a un medicamento vencido.");
        }
        medicamento.retirarStock(cantidad);
        repositorio.actualizarStock(id, medicamento.getCantidadStock());
        return medicamento;
    }

    /** Medicamentos cuyo stock es igual o menor que su stock mínimo. */
    public List<Medicamento> listarStockBajo() {
        return repositorio.listarTodos().stream()
                .filter(Medicamento::tieneStockBajo)
                .toList();
    }

    /** No vencidos que vencen entre hoy y hoy + {@code dias}, del más próximo al más lejano. */
    public List<Medicamento> listarPorVencer(int dias) {
        if (dias < DIAS_POR_VENCER_MIN || dias > DIAS_POR_VENCER_MAX) {
            throw new SolicitudInvalidaException("El parámetro dias debe ser un entero entre "
                    + DIAS_POR_VENCER_MIN + " y " + DIAS_POR_VENCER_MAX + ".");
        }
        LocalDate hoy = hoy();
        LocalDate limite = hoy.plusDays(dias);
        return repositorio.listarTodos().stream()
                .filter(m -> !m.estaVencido(hoy) && !m.getDatos().fechaVencimiento().isAfter(limite))
                .sorted(POR_VENCIMIENTO)
                .toList();
    }

    /** Medicamentos cuya fecha de vencimiento ya pasó, del más antiguo al más reciente. */
    public List<Medicamento> listarVencidos() {
        LocalDate hoy = hoy();
        return repositorio.listarTodos().stream()
                .filter(m -> m.estaVencido(hoy))
                .sorted(POR_VENCIMIENTO)
                .toList();
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Lee y bloquea el medicamento hasta el final de la transacción; 404 si no existe. */
    private Medicamento obtenerParaActualizar(String id) {
        Medicamento medicamento = repositorio.buscarPorIdParaActualizar(id);
        if (medicamento == null) {
            throw noEncontrado(id);
        }
        return medicamento;
    }

    private static DatosMedicamento recortar(DatosMedicamento d) {
        return new DatosMedicamento(d.nombre().trim(), d.principioActivo().trim(), d.presentacion(),
                d.concentracion().trim(), d.laboratorio().trim(), d.lote().trim(), d.stockMinimo(),
                d.fechaVencimiento(), d.ubicacion().trim());
    }

    private static boolean contiene(String valor, String filtro) {
        return valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private NotFoundException noEncontrado(String id) {
        return new NotFoundException("No se encontró el medicamento " + id + ".");
    }

    private ConflictoException duplicado() {
        return new ConflictoException(
                "Ya existe un medicamento con el mismo nombre, concentración, presentación y lote.");
    }
}
