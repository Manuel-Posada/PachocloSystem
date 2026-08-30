package view;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import controller.ControladorHistorialClinico;
import controller.ControladorTrabajadores;
import model.TipoRegistro;
import model.TrabajadorHospital;
import model.RegistroClinico;

public class GUIHistorialClinico extends JFrame implements IGUIHistorialClinico {

    private IGUIPrincipal guiPrincipal;
    private final ControladorHistorialClinico controlador;
    private final ControladorTrabajadores controladorTrabajadores;

    public GUIHistorialClinico(ControladorHistorialClinico controlador, ControladorTrabajadores controladorTrabajadores) {
        this.controlador = controlador;
        this.controladorTrabajadores = controladorTrabajadores;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Historial Clínico");
        setSize(420, 300);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    @Override
    public void mostrar() {
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JButton btnAgregar = new JButton("Agregar Registro Clínico");
        JButton btnVer = new JButton("Ver Historial Clínico");
        JButton btnVolver = new JButton("Volver");

        btnAgregar.addActionListener(e -> agregarRegistroClinico());
        btnVer.addActionListener(e -> verHistorialClinico());
        btnVolver.addActionListener(e -> volver());

        panel.add(btnAgregar);
        panel.add(btnVer);
        panel.add(btnVolver);

        setContentPane(panel);
        revalidate();
        repaint();
    }

    @Override
    public void agregarRegistroClinico() {
        String idPaciente = JOptionPane.showInputDialog(this, "ID del paciente:");
        if (idPaciente == null || idPaciente.isBlank()) return;

        TipoRegistro tipo = (TipoRegistro) JOptionPane.showInputDialog(
                this, "Tipo de registro:", "Registro Clínico",
                JOptionPane.QUESTION_MESSAGE, null,
                TipoRegistro.values(), TipoRegistro.values()[0]);
        if (tipo == null) return;

        JTextArea areaContenido = new JTextArea(6, 30);
        int opcion = JOptionPane.showConfirmDialog(this, new JScrollPane(areaContenido),
                "Contenido del registro", JOptionPane.OK_CANCEL_OPTION);
        if (opcion != JOptionPane.OK_OPTION) return;
        String contenido = areaContenido.getText().trim();
        if (contenido.isBlank()) {
            JOptionPane.showMessageDialog(this, "El contenido no puede estar vacío.");
            return;
        }

        String idAutor = JOptionPane.showInputDialog(this, "ID del trabajador que registra (autor):");
        if (idAutor == null || idAutor.isBlank()) return;

        TrabajadorHospital autor = controladorTrabajadores.buscarTrabajadorPorId(idAutor);
        if (autor == null) {
            JOptionPane.showMessageDialog(this, "No se encontró un trabajador con ese ID.");
            return;
        }

        boolean ok = controlador.agregarRegistroPaciente(idPaciente, tipo, contenido, autor);
        JOptionPane.showMessageDialog(this, ok
                ? "Registro agregado correctamente."
                : "No se pudo agregar el registro: paciente no encontrado.");
    }

    @Override
    public void verHistorialClinico() {
        String idPaciente = JOptionPane.showInputDialog(this, "ID del paciente:");
        if (idPaciente == null || idPaciente.isBlank()) return;

        List<RegistroClinico> historial = controlador.obtenerHistorialPaciente(idPaciente);
        if (historial == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el paciente.");
            return;
        }
        if (historial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El paciente no tiene registros clínicos.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (RegistroClinico r : historial) {
            sb.append(r).append("\n");
        }

        JTextArea areaHistorial = new JTextArea(sb.toString(), 15, 40);
        areaHistorial.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(areaHistorial), "Historial Clínico", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}