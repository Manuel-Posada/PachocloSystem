package view;

public interface IGUIHistorialClinico {
    void mostrar(); // <- Agregamos esta línea para el historial general
    void mostrar(String idPaciente, String nombrePaciente); // Para el historial por paciente
    void mostrarOpciones(); // (La agrego por si la heredabas de otra parte, ya que la clase la usa con @Override)
    void agregarRegistroClinico();
    void verHistorialClinico();
    void volver();
}