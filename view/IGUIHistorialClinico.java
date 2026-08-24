
package view;

/**
 * GUI dedicada al historial clínico de un paciente (agregar y
 * consultar registros). La implementación concreta recibe un
 * ControladorHistorialClinico (inyectado por constructor).
 */
public interface IGUIHistorialClinico {

    /** Hace visible la ventana (o la trae al frente). */
    void mostrar();

    /** Renderiza las opciones propias de esta pantalla. */
    void mostrarOpciones();

    /**
     * Captura idPaciente, tipo, contenido y autor; delega en
     * controlador.agregarRegistroPaciente(...).
     */
    void agregarRegistroClinico();

    /**
     * Captura idPaciente; delega en controlador.obtenerHistorialPaciente(...)
     * y renderiza la lista de RegistroClinico resultante.
     */
    void verHistorialClinico();

    /** Retorna el control a la GUI principal. */
    void volver();
}