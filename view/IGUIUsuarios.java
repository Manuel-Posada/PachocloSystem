package view;

/**
 * GUI para el CRUD de usuarios/trabajadores del hospital.
 * La implementación concreta recibe un ControladorUsuarios
 * (inyectado por constructor) para ejecutar cada operación.
 */
public interface IGUIUsuarios {

    /** Hace visible la ventana (o la trae al frente). */
    void mostrar();

    /** Renderiza las opciones propias de esta pantalla. */
    void mostrarOpciones();

    /** Captura id, nombre y rol; delega en controlador.registrarUsuario(...). */
    void registrarUsuario();

    /** Captura id y nuevos datos; delega en controlador.editarUsuario(...). */
    void editarUsuario();

    /** Captura id; delega en controlador.eliminarUsuario(...). */
    void eliminarUsuario();

    /** Delega en controlador.listarUsuarios() y renderiza el resultado. */
    void listarUsuarios();

    /** Retorna el control a la GUI principal. */
    void volver();
}