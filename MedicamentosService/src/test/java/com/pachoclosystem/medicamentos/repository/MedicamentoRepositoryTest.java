package com.pachoclosystem.medicamentos.repository;

import com.pachoclosystem.medicamentos.model.DatosMedicamento;
import com.pachoclosystem.medicamentos.model.Medicamento;
import com.pachoclosystem.medicamentos.model.Presentacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Almacenamiento en memoria: ids, unicidad de la clave y mantenimiento del índice. */
class MedicamentoRepositoryTest {

    private MedicamentoRepositoryImpl repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new MedicamentoRepositoryImpl();
    }

    private static DatosMedicamento datos(String lote, Presentacion presentacion) {
        return new DatosMedicamento("Dolex", "Paracetamol", presentacion, "500 mg", "Genfar", lote, 10,
                LocalDate.of(2027, 1, 31), "Estante A3");
    }

    private Medicamento nuevo(String lote) {
        return new Medicamento(repositorio.generarNuevoId(), datos(lote, Presentacion.TABLETA), 10);
    }

    @Test
    void generaIdsConsecutivosConFormatoMed() {
        assertThat(repositorio.generarNuevoId()).isEqualTo("MED-0001");
        assertThat(repositorio.generarNuevoId()).isEqualTo("MED-0002");
    }

    @Test
    void guardarRechazaOtroConLaMismaClave() {
        assertThat(repositorio.guardar(nuevo("L-1"))).isTrue();

        assertThat(repositorio.guardar(nuevo("L-1"))).isFalse();
        assertThat(repositorio.listarTodos()).hasSize(1);
    }

    @Test
    void laClaveNoDistingueMayusculasNiEspaciosRepetidos() {
        repositorio.guardar(nuevo("Lote A"));
        Medicamento variante = new Medicamento(repositorio.generarNuevoId(),
                new DatosMedicamento("DOLEX", "Paracetamol", Presentacion.TABLETA, "500   MG", "Otro",
                        "lote a", 1, LocalDate.of(2027, 1, 31), "Otro"), 1);

        assertThat(repositorio.guardar(variante)).isFalse();
    }

    @Test
    void distintaPresentacionEsOtraClave() {
        repositorio.guardar(nuevo("L-1"));
        Medicamento jarabe = new Medicamento(repositorio.generarNuevoId(), datos("L-1", Presentacion.JARABE), 1);

        assertThat(repositorio.guardar(jarabe)).isTrue();
    }

    @Test
    void volverAGuardarElMismoMedicamentoEsValido() {
        Medicamento medicamento = nuevo("L-1");
        repositorio.guardar(medicamento);

        assertThat(repositorio.guardar(medicamento)).isTrue();
    }

    @Test
    void actualizarDatosLiberaLaClaveVieja() {
        Medicamento medicamento = nuevo("L-1");
        repositorio.guardar(medicamento);

        assertThat(repositorio.actualizarDatos(medicamento, datos("L-2", Presentacion.TABLETA))).isTrue();

        assertThat(medicamento.getDatos().lote()).isEqualTo("L-2");
        assertThat(repositorio.guardar(nuevo("L-1"))).isTrue();
        assertThat(repositorio.guardar(nuevo("L-2"))).isFalse();
    }

    @Test
    void actualizarDatosHaciaUnaClaveOcupadaNoCambiaNada() {
        repositorio.guardar(nuevo("L-1"));
        Medicamento otro = nuevo("L-2");
        repositorio.guardar(otro);

        assertThat(repositorio.actualizarDatos(otro, datos("L-1", Presentacion.TABLETA))).isFalse();

        assertThat(otro.getDatos().lote()).isEqualTo("L-2");
        assertThat(repositorio.guardar(nuevo("L-2"))).isFalse();
    }

    @Test
    void eliminarQuitaElMedicamentoYLiberaSuClave() {
        Medicamento medicamento = nuevo("L-1");
        repositorio.guardar(medicamento);

        assertThat(repositorio.eliminar(medicamento.getIdMedicamento())).isTrue();

        assertThat(repositorio.buscarPorId(medicamento.getIdMedicamento())).isNull();
        assertThat(repositorio.eliminar(medicamento.getIdMedicamento())).isFalse();
        assertThat(repositorio.guardar(nuevo("L-1"))).isTrue();
    }

    @Test
    void listarTodosDevuelveOrdenadoPorId() {
        Medicamento primero = nuevo("L-1");
        Medicamento segundo = nuevo("L-2");
        repositorio.guardar(segundo);
        repositorio.guardar(primero);

        assertThat(repositorio.listarTodos()).containsExactly(primero, segundo);
    }

    @Test
    void buscarConIdNuloDevuelveNulo() {
        assertThat(repositorio.buscarPorId(null)).isNull();
        assertThat(repositorio.eliminar(null)).isFalse();
    }
}
