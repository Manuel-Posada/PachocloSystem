import javax.swing.SwingUtilities;

import controller.ControladorHistorialClinico;
import controller.ControladorPaciente;
import controller.ControladorTrabajadores;
import model.IPacienteRepository;
import model.ITrabajadoresRepository;
import model.PacienteRepositoryImpl;
import model.TrabajadorRepositoryImpl;
import view.GUIEditarTrabajadores;
import view.GUIEliminarTrabajadores;
import view.GUIHistorialClinico;
import view.GUIPacientes;
import view.GUIPrincipal;
import view.GUIRegistrarTrabajador;
import view.GUIVerTrabajadores;

class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            //Repositorios (instancia normal, una sola vez)
            IPacienteRepository repoPacientes = new PacienteRepositoryImpl();
            ITrabajadoresRepository repoTrabajadores = new TrabajadorRepositoryImpl();

            //Controladores
            ControladorTrabajadores ctrlTrabajadores = new ControladorTrabajadores(repoTrabajadores);
            ControladorPaciente ctrlPaciente = new ControladorPaciente(repoPacientes);
            ControladorHistorialClinico ctrlHistorial = new ControladorHistorialClinico(repoPacientes);

            //Vistas de Trabajadores: una por responsabilidad del CRUD
            GUIRegistrarTrabajador guiRegistrarTrabajador = new GUIRegistrarTrabajador(ctrlTrabajadores);
            GUIVerTrabajadores guiVerTrabajadores = new GUIVerTrabajadores(ctrlTrabajadores);
            GUIEditarTrabajadores guiEditarTrabajadores = new GUIEditarTrabajadores(ctrlTrabajadores);
            GUIEliminarTrabajadores guiEliminarTrabajadores = new GUIEliminarTrabajadores(ctrlTrabajadores);

            //otras vistas
            GUIPacientes guiPacientes = new GUIPacientes(ctrlPaciente);
            // GUIHistorialClinico necesita también ctrlTrabajadores para buscar
            // al trabajador que figura como "autor" del registro clínico
            // (todavía no hay sesión/login, así que se pide el ID manualmente).
            GUIHistorialClinico guiHistorial = new GUIHistorialClinico(ctrlHistorial, ctrlTrabajadores);

            GUIPrincipal guiPrincipal = new GUIPrincipal(
                    guiRegistrarTrabajador, guiVerTrabajadores, guiEditarTrabajadores, guiEliminarTrabajadores,
                    guiPacientes, guiHistorial);

            //Referencia inversa (hijas -> principal) resuelta con setter injection
            guiRegistrarTrabajador.setGuiPrincipal(guiPrincipal);
            guiVerTrabajadores.setGuiPrincipal(guiPrincipal);
            guiEditarTrabajadores.setGuiPrincipal(guiPrincipal);
            guiEliminarTrabajadores.setGuiPrincipal(guiPrincipal);
            guiPacientes.setGuiPrincipal(guiPrincipal);
            guiHistorial.setGuiPrincipal(guiPrincipal);

            guiPrincipal.mostrar();
        });
    }
}