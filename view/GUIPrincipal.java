package view;

import javax.swing.*;
import java.awt.*;

public class GUIPrincipal extends JFrame implements IGUIPrincipal {

    private final IguiTrabajadores guiTrabajadores;
    private final IGUIPacientes guiPacientes;
    private final IGUIHistorialClinico guiHistorial;

    public GUIPrincipal(IguiTrabajadores guiTrabajadores,
                         IGUIPacientes guiPacientes,
                         IGUIHistorialClinico guiHistorial) {
        this.guiTrabajadores = guiTrabajadores;
        this.guiPacientes = guiPacientes;
        this.guiHistorial = guiHistorial;

        configurarVentana();
        mostrarOpciones();
    }

    private void configurarVentana() {
        setTitle("Sistema Hospitalario - Menú Principal");
        setSize(500, 380);
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
        JMenuBar menuBar = new JMenuBar();

        //menu de trabajadores
        JMenu menuTrabajadores = new JMenu("Trabajadores");
        JMenuItem itemVerTrabajadores = new JMenuItem("Ver Trabajadores");
        itemVerTrabajadores.addActionListener(e -> irAGestionTrabajadors());
        menuTrabajadores.add(itemVerTrabajadores);

        //menu de pacientes
        JMenu menuPacientes = new JMenu("Pacientes");
        JMenuItem itemVerPacientes = new JMenuItem("Ver Pacientes");
        itemVerPacientes.addActionListener(e -> irAGestionPacientes());
        menuPacientes.add(itemVerPacientes);

        //menu del historial clinico
        JMenu menuHistorial = new JMenu("Historial Clínico");
        JMenuItem itemVerHistorial = new JMenuItem("Ver Historial");
        itemVerHistorial.addActionListener(e -> irAHistorialClinico());
        menuHistorial.add(itemVerHistorial);

        menuBar.add(menuTrabajadores);
        menuBar.add(menuPacientes);
        menuBar.add(menuHistorial);

        //se edito el boton de salir para no perder tiempo 
        menuBar.add(Box.createHorizontalGlue());
        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> salir());
        menuBar.add(btnSalir);

        setJMenuBar(menuBar);

        JLabel bienvenida = new JLabel("Sistema Hospitalario", SwingConstants.CENTER);
        bienvenida.setFont(bienvenida.getFont().deriveFont(Font.BOLD, 20f));
        setContentPane(bienvenida);

        revalidate();
        repaint();
    }

    @Override
    public void irAGestionTrabajadors() {
        setVisible(false);
        guiTrabajadores.mostrar();
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