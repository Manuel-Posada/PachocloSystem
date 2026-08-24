package view;

/**
 * GUI para el CRUD básico de pacientes (sin historial clínico,
 * que queda a cargo de IGUIHistorialClinico).
 * La implementación concreta recibe un ControladorPaciente
 * (inyectado por constructor) para ejecutar cada operación.
 */
public interface IGUIPacientes {

    /** Hace visible la ventana (o la trae al frente). */
    void mostrar();

    /** Renderiza las opciones propias de esta pantalla. */
    void mostrarOpciones();

    /** Captura id, nombre, edad, habitación; delega en controlador.registrarPaciente(...). */
    void registrarPaciente();

    /** Captura id y nueva habitación; delega en controlador.editarPaciente(...). */
    void editarPaciente();

    /** Captura id; delega en controlador.eliminarPaciente(...). */
    void eliminarPaciente();

    /** Delega en controlador.listarPacientes() y renderiza el resultado. */
    void listarPacientes();

    /** Captura un id y delega en controlador.buscarPorId(...) para validar existencia. */
    void buscarPaciente();

    /** Retorna el control a la GUI principal. */
    void volver();
}