package view;

import javax.swing.*;
import java.awt.*;

/**
 * Implementación Swing del menú principal. No conoce controladores
 * ni repositorios: solo recibe (por constructor) las tres pantallas
 * hijas ya construidas y decide cuándo mostrarlas/ocultarlas.
 */
public class GUIPrincipal extends JFrame implements IGUIPrincipal {

    private final IGUIUsuarios guiUsuarios;
    private final IGUIPacientes guiPacientes;
    private final IGUIHistorialClinico guiHistorial;

    public GUIPrincipal(IGUIUsuarios guiUsuarios,
                         IGUIPacientes guiPacientes,
                         IGUIHistorialClinico guiHistorial) {
        this.guiUsuarios = guiUsuarios;
        this.guiPacientes = guiPacientes;
        this.guiHistorial = guiHistorial;

        configurarVentana();
        mostrarOpciones();
    }

    private void configurarVentana() {
        setTitle("Sistema Hospitalario - Menú Principal");
        setSize(420, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    @Override
    public void mostrar() {
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel titulo = new JLabel("Seleccione una opción", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JButton btnUsuarios = new JButton("Gestión de Usuarios");
        JButton btnPacientes = new JButton("Gestión de Pacientes");
        JButton btnHistorial = new JButton("Historial Clínico");
        JButton btnSalir = new JButton("Salir");

        btnUsuarios.addActionListener(e -> irAGestionUsuarios());
        btnPacientes.addActionListener(e -> irAGestionPacientes());
        btnHistorial.addActionListener(e -> irAHistorialClinico());
        btnSalir.addActionListener(e -> salir());

        panel.add(titulo);
        panel.add(btnUsuarios);
        panel.add(btnPacientes);
        panel.add(btnHistorial);
        panel.add(btnSalir);

        setContentPane(panel);
        revalidate();
        repaint();
    }

    @Override
    public void irAGestionUsuarios() {
        setVisible(false);
        guiUsuarios.mostrar();
    }

    @Override
    public void irAGestionPacientes() {
        setVisible(false);
        guiPacientes.mostrar();
    }

    @Override
    public void irAHistorialClinico() {
        setVisible(false);
        guiHistorial.mostrar();
    }

    @Override
    public void salir() {
        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea salir del sistema?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirmacion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}