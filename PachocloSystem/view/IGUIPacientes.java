package view;

public interface IGUIPacientes {

    void mostrar();

    void mostrarOpciones();

    void refrescarTabla();

    void registrarPaciente();

    void editarPaciente(String idPaciente);

    void eliminarPaciente(String idPaciente);

    void volver();
}