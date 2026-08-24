import javax.swing.SwingUtilities;

import controller.ControladorHistorialClinico;
import controller.ControladorPaciente;
import controller.ControladorUsuarios;
import model.IPacienteRepository;
import model.IUsuarioRepository;
import model.PacienteRepositoryImpl;
import model.UsuarioRepositoryImpl;
import view.GUIHistorialClinico;
import view.GUIPacientes;
import view.GUIPrincipal;
import view.GUIUsuarios;

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
            IUsuarioRepository repoUsuarios = new UsuarioRepositoryImpl();

            // --- Controladores ---
            ControladorUsuarios ctrlUsuarios = new ControladorUsuarios(repoUsuarios);
            ControladorPaciente ctrlPaciente = new ControladorPaciente(repoPacientes);
            ControladorHistorialClinico ctrlHistorial = new ControladorHistorialClinico(repoPacientes);

            // --- Vistas (cada una recibe su controlador) ---
            GUIUsuarios guiUsuarios = new GUIUsuarios(ctrlUsuarios);
            GUIPacientes guiPacientes = new GUIPacientes(ctrlPaciente);
            GUIHistorialClinico guiHistorial = new GUIHistorialClinico(ctrlHistorial);

            GUIPrincipal guiPrincipal = new GUIPrincipal(guiUsuarios, guiPacientes, guiHistorial);

            // --- Referencia inversa (hijas -> principal) resuelta con setter injection ---
            guiUsuarios.setGuiPrincipal(guiPrincipal);
            guiPacientes.setGuiPrincipal(guiPrincipal);
            guiHistorial.setGuiPrincipal(guiPrincipal);

            guiPrincipal.mostrar();
        });
    }
}