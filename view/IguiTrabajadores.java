package view;

/**
 * GUI para el CRUD de trabajadors/trabajadores del hospital.
 * La implementación concreta recibe un ControladorTrabajadores
 * (inyectado por constructor) para ejecutar cada operación.
 */
public interface IguiTrabajadores {

    /** Hace visible la ventana (o la trae al frente). */
    void mostrar();

    /** Renderiza las opciones propias de esta pantalla. */
    void mostrarOpciones();

    /** Captura id, nombre y rol; delega en controlador.registrarTrabajador(...). */
    void registrarTrabajador();

    /** Captura id y nuevos datos; delega en controlador.editarTrabajador(...). */
    void editarTrabajador();

    /** Captura id; delega en controlador.eliminarTrabajador(...). */
    void eliminarTrabajador();

    /** Delega en controlador.listarTrabajadores() y renderiza el resultado. */
    void listarTrabajadores();

    /** Retorna el control a la GUI principal. */
    void volver();
}