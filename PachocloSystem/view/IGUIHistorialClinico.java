package view;

public interface IGUIHistorialClinico {
    
    void mostrar(); // Para el historial general (todos los pacientes)
    
    void mostrar(String idPaciente, String nombrePaciente); // Para el historial de un paciente específico

    void mostrarOpciones(); // (La agrego por si la heredabas de otra parte, ya que la clase la usa con @Override)

    void agregarRegistroClinico();

    void verHistorialClinico();

    void volver();
}