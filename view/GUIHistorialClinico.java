package view;

import javax.swing.*;
import java.awt.*;

import controller.ControladorHistorialClinico;

/**
 * Esqueleto navegable de la pantalla de Historial Clínico.
 * TODO: reemplazar el contenido de mostrarOpciones() por los
 * formularios reales (agregar registro, ver historial),
 * apoyándose en ControladorHistorialClinico.
 */
public class GUIHistorialClinico extends JFrame implements IGUIHistorialClinico {

    private IGUIPrincipal guiPrincipal;
    private final ControladorHistorialClinico controlador;

    public GUIHistorialClinico(ControladorHistorialClinico controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    /** Setter injection: se asigna después de construir GUIPrincipal, evitando dependencia circular en el constructor. */
    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Historial Clínico");
        setSize(420, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        JOptionPane.showMessageDialog(this, "TODO: formulario para agregar un registro clínico");
    }

    @Override
    public void verHistorialClinico() {
        JOptionPane.showMessageDialog(this, "TODO: listado del historial clínico del paciente");
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}