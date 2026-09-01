package view;

//NO CONOCE controladores ni repositorios directamente: solo orquesta la navegación hacia las pantallas especializadas
public interface IGUIPrincipal {

    void mostrar();

    void mostrarOpciones();

    void irARegistrarTrabajador();

    void irAVerTrabajadores();

    void irAEditarTrabajadores();

    void irAEliminarTrabajadores();

    void irAGestionPacientes();

    void irAHistorialClinico();

    void salir();
}