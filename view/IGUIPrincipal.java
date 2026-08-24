package view;

/**
 * GUI raíz de la aplicación. Orquesta la navegación hacia las
 * pantallas especializadas (Usuarios, Pacientes, Historial Clínico).
 * No conoce controladores ni repositorios directamente.
 */
public interface IGUIPrincipal {

    /** Hace visible la ventana (o la trae al frente). */
    void mostrar();

    /** Renderiza las opciones del menú principal. */
    void mostrarOpciones();

    /** Deriva el control hacia la GUI de gestión de usuarios. */
    void irAGestionUsuarios();

    /** Deriva el control hacia la GUI de gestión de pacientes. */
    void irAGestionPacientes();

    /** Deriva el control hacia la GUI de historial clínico. */
    void irAHistorialClinico();

    /** Finaliza la ejecución de la aplicación. */
    void salir();
}