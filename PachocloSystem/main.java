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

class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            IPacienteRepository repoPacientes = new PacienteRepositoryImpl();
            ITrabajadoresRepository repotrabajadores = new TrabajadorRepositoryImpl();

            ControladorTrabajadores ctrlTrabajadores = new ControladorTrabajadores(repotrabajadores);
            ControladorPaciente ctrlPaciente = new ControladorPaciente(repoPacientes);
            ControladorHistorialClinico ctrlHistorial = new ControladorHistorialClinico(repoPacientes);

            GUITrabajadores guiTrabajadores = new GUITrabajadores(ctrlTrabajadores);
            GUIPacientes guiPacientes = new GUIPacientes(ctrlPaciente);
            GUIHistorialClinico guiHistorial = new GUIHistorialClinico(ctrlHistorial, ctrlTrabajadores);

            GUIPrincipal guiPrincipal = new GUIPrincipal(guiTrabajadores, guiPacientes, guiHistorial);

            guiTrabajadores.setGuiPrincipal(guiPrincipal);
            guiPacientes.setGuiPrincipal(guiPrincipal);
            guiHistorial.setGuiPrincipal(guiPrincipal);
            guiPacientes.setGuiHistorial(guiHistorial);
            guiHistorial.setGuiPacientes(guiPacientes);
            guiPrincipal.mostrar();
        });
    }
}