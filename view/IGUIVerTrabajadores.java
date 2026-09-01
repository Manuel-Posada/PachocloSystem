package view;

//pantalla de solo lectura: lista tipo Excel de trabajadores + busqueda por ID
public interface IGUIVerTrabajadores {

    void mostrar();

    void mostrarOpciones();

    void buscarPorId();

    void mostrarTodos();

    void volver();
}