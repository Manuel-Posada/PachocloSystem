import javax.swing.SwingUtilities;

import controller.ControladorHistorialClinico;
import controller.ControladorPaciente;
import controller.ControladorTrabajadores;
import model.IPacienteRepository;
import model.ITrabajadoresRepository;
import model.PacienteRepositoryImpl;
import model.TrabajadorRepositoryImpl;
import view.GUIHistorialClinico;
import view.GUIPacientes;
import view.GUIPrincipal;
import view.GUITrabajadores;

/**
 * Punto de entrada. Ensambla repositorios -> controladores -> vistas,
 * sin Singleton: cada repositorio se crea una sola vez aquí y se
 * comparte por inyección con quien lo necesite.
 *
 * repoPacientes se pasa TANTO a ControladorPaciente como a
 * ControladorHistorialClinico porque ambos operan sobre los mismos
 * datos de Paciente (uno gestiona la entidad, el otro su historial).
 */
class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // --- Repositorios (instancia normal, una sola vez) ---
            IPacienteRepository repoPacientes = new PacienteRepositoryImpl();
            ITrabajadoresRepository repoTrabajadors = new TrabajadorRepositoryImpl();

            // --- Controladores ---
            ControladorTrabajadores ctrlTrabajadores = new ControladorTrabajadores(repoTrabajadors);
            ControladorPaciente ctrlPaciente = new ControladorPaciente(repoPacientes);
            ControladorHistorialClinico ctrlHistorial = new ControladorHistorialClinico(repoPacientes);

            // --- Vistas (cada una recibe su controlador) ---
            GUITrabajadores guiTrabajadores = new GUITrabajadores(ctrlTrabajadores);
            GUIPacientes guiPacientes = new GUIPacientes(ctrlPaciente);
            // GUIHistorialClinico necesita también ctrlTrabajadores para buscar
            // al trabajador que figura como "autor" del registro clínico
            // (todavía no hay sesión/login, así que se pide el ID manualmente).
            GUIHistorialClinico guiHistorial = new GUIHistorialClinico(ctrlHistorial, ctrlTrabajadores);

            GUIPrincipal guiPrincipal = new GUIPrincipal(guiTrabajadores, guiPacientes, guiHistorial);

            // --- Referencia inversa (hijas -> principal) resuelta con setter injection ---
            guiTrabajadores.setGuiPrincipal(guiPrincipal);
            guiPacientes.setGuiPrincipal(guiPrincipal);
            guiHistorial.setGuiPrincipal(guiPrincipal);

            guiPrincipal.mostrar();
        });
    }
}