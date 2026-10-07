package com.pachoclosystem.medicamentos.model;

import com.pachoclosystem.medicamentos.exception.SolicitudInvalidaException;

import java.time.LocalDate;

public class Medicamento {

    private final String idMedicamento;
    private volatile DatosMedicamento datos;
    private volatile int cantidadStock;

    public Medicamento(String idMedicamento, DatosMedicamento datos, int cantidadStock) {
        this.idMedicamento = idMedicamento;
        this.datos = datos;
        this.cantidadStock = cantidadStock;
    }

    /** Suma unidades al stock. */
    public synchronized void ingresarStock(int cantidad) {
        try {
            cantidadStock = Math.addExact(cantidadStock, cantidad);
        } catch (ArithmeticException ex) {
            throw new SolicitudInvalidaException("La cantidad supera el stock máximo admitido.");
        }
    }

    /**
     * Resta unidades del stock si alcanzan. Entre peticiones, lo que impide que
     * dos salidas simultáneas dejen el stock en negativo es el bloqueo de fila
     * con el que {@code MedicamentoService} lee el medicamento; la restricción
     * {@code cantidad_stock >= 0} de la base es la última defensa.
     */
    public synchronized void retirarStock(int cantidad) {
        if (cantidad > cantidadStock) {
            throw new SolicitudInvalidaException("Stock insuficiente: disponible "
                    + cantidadStock + ", solicitado " + cantidad + ".");
        }
        cantidadStock -= cantidad;
    }

    public boolean tieneStockBajo() {
        return cantidadStock <= datos.stockMinimo();
    }

    public boolean estaVencido(LocalDate hoy) {
        return datos.fechaVencimiento().isBefore(hoy);
    }

    public String getIdMedicamento() {
        return idMedicamento;
    }

    public DatosMedicamento getDatos() {
        return datos;
    }

    /** Lo usa el repositorio, que es quien garantiza la unicidad de los datos. */
    public void setDatos(DatosMedicamento datos) {
        this.datos = datos;
    }

    public int getCantidadStock() {
        return cantidadStock;
    }

    @Override
    public String toString() {
        return "ID: " + idMedicamento + " | " + datos.nombre() + " " + datos.concentracion()
                + " | Lote: " + datos.lote() + " | Stock: " + cantidadStock;
    }
}
