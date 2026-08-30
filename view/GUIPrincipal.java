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

        // ---- Menú Trabajadores ----
        JMenu menuTrabajadores = new JMenu("Trabajadores");
        JMenuItem itemRegistrarTrabajador = new JMenuItem("Registrar");
        JMenuItem itemEditarTrabajador = new JMenuItem("Editar");
        JMenuItem itemEliminarTrabajador = new JMenuItem("Eliminar");
        JMenuItem itemListarTrabajadores = new JMenuItem("Listar");

        itemRegistrarTrabajador.addActionListener(e -> irAGestionTrabajadors());
        itemEditarTrabajador.addActionListener(e -> irAGestionTrabajadors());
        itemEliminarTrabajador.addActionListener(e -> irAGestionTrabajadors());
        itemListarTrabajadores.addActionListener(e -> irAGestionTrabajadors());

        menuTrabajadores.add(itemRegistrarTrabajador);
        menuTrabajadores.add(itemEditarTrabajador);
        menuTrabajadores.add(itemEliminarTrabajador);
        menuTrabajadores.add(itemListarTrabajadores);

        // ---- Menú Pacientes ----
        JMenu menuPacientes = new JMenu("Pacientes");
        JMenuItem itemRegistrarPaciente = new JMenuItem("Registrar");
        JMenuItem itemEditarPaciente = new JMenuItem("Editar");
        JMenuItem itemEliminarPaciente = new JMenuItem("Eliminar");
        JMenuItem itemListarPacientes = new JMenuItem("Listar");
        JMenuItem itemBuscarPaciente = new JMenuItem("Buscar");

        itemRegistrarPaciente.addActionListener(e -> irAGestionPacientes());
        itemEditarPaciente.addActionListener(e -> irAGestionPacientes());
        itemEliminarPaciente.addActionListener(e -> irAGestionPacientes());
        itemListarPacientes.addActionListener(e -> irAGestionPacientes());
        itemBuscarPaciente.addActionListener(e -> irAGestionPacientes());

        menuPacientes.add(itemRegistrarPaciente);
        menuPacientes.add(itemEditarPaciente);
        menuPacientes.add(itemEliminarPaciente);
        menuPacientes.add(itemListarPacientes);
        menuPacientes.add(itemBuscarPaciente);

        // ---- Menú Historial Clínico ----
        JMenu menuHistorial = new JMenu("Historial Clínico");
        JMenuItem itemVerHistorial = new JMenuItem("Ver Historial");
        itemVerHistorial.addActionListener(e -> irAHistorialClinico());
        menuHistorial.add(itemVerHistorial);

        // ---- Salir ----
        JMenu menuSalir = new JMenu("Salir");
        JMenuItem itemSalir = new JMenuItem("Salir del sistema");
        itemSalir.addActionListener(e -> salir());
        menuSalir.add(itemSalir);

        menuBar.add(menuTrabajadores);
        menuBar.add(menuPacientes);
        menuBar.add(menuHistorial);
        menuBar.add(menuSalir);

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