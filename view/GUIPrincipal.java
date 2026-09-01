package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GUIPrincipal extends JFrame implements IGUIPrincipal {

    private final IGUIRegistrarTrabajador guiRegistrarTrabajador;
    private final IGUIVerTrabajadores guiVerTrabajadores;
    private final IGUIEditarTrabajadores guiEditarTrabajadores;
    private final IGUIEliminarTrabajadores guiEliminarTrabajadores;
    private final IGUIPacientes guiPacientes;
    private final IGUIHistorialClinico guiHistorial;

    public GUIPrincipal(IGUIRegistrarTrabajador guiRegistrarTrabajador,
                         IGUIVerTrabajadores guiVerTrabajadores,
                         IGUIEditarTrabajadores guiEditarTrabajadores,
                         IGUIEliminarTrabajadores guiEliminarTrabajadores,
                         IGUIPacientes guiPacientes,
                         IGUIHistorialClinico guiHistorial) {
        this.guiRegistrarTrabajador = guiRegistrarTrabajador;
        this.guiVerTrabajadores = guiVerTrabajadores;
        this.guiEditarTrabajadores = guiEditarTrabajadores;
        this.guiEliminarTrabajadores = guiEliminarTrabajadores;
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

        // menú de trabajadores: se despliega al pasar el mouse por encima
        // (sin necesidad de hacer clic), con las 4 responsabilidades del
        // CRUD separadas cada una en su propia pantalla.
        JMenu menuTrabajadores = new JMenu("Trabajadores");

        JMenuItem itemRegistrar = new JMenuItem("Registrar Nuevo Trabajador");
        itemRegistrar.addActionListener(e -> irARegistrarTrabajador());

        JMenuItem itemVer = new JMenuItem("Ver / Buscar Trabajadores");
        itemVer.addActionListener(e -> irAVerTrabajadores());

        JMenuItem itemEditar = new JMenuItem("Editar Trabajadores");
        itemEditar.addActionListener(e -> irAEditarTrabajadores());

        JMenuItem itemEliminar = new JMenuItem("Eliminar Trabajadores");
        itemEliminar.addActionListener(e -> irAEliminarTrabajadores());

        menuTrabajadores.add(itemRegistrar);
        menuTrabajadores.add(itemVer);
        menuTrabajadores.add(itemEditar);
        menuTrabajadores.add(itemEliminar);

        // menú de pacientes
        JMenu menuPacientes = new JMenu("Pacientes");
        JMenuItem itemVerPacientes = new JMenuItem("Ver Pacientes");
        itemVerPacientes.addActionListener(e -> irAGestionPacientes());
        menuPacientes.add(itemVerPacientes);

        // menú del historial clínico
        JMenu menuHistorial = new JMenu("Historial Clínico");
        JMenuItem itemVerHistorial = new JMenuItem("Ver Historial");
        itemVerHistorial.addActionListener(e -> irAHistorialClinico());
        menuHistorial.add(itemVerHistorial);

        menuBar.add(menuTrabajadores);
        menuBar.add(menuPacientes);
        menuBar.add(menuHistorial);

        // los 3 menús se pueden abrir pasando el mouse por encima,
        // sin necesidad de hacer clic primero (ver habilitarAperturaConHover)
        habilitarAperturaConHover(menuBar, menuTrabajadores);
        habilitarAperturaConHover(menuBar, menuPacientes);
        habilitarAperturaConHover(menuBar, menuHistorial);

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

    /**
     * Truco estándar de Swing para que un JMenu se despliegue al pasar
     * el mouse por encima (hover), sin necesidad de hacer clic primero.
     * Al entrar el mouse en el título del menú, se fuerza su selección
     * en el MenuSelectionManager, que es quien realmente controla qué
     * popup está abierto en la barra de menús.
     */
    private void habilitarAperturaConHover(JMenuBar barra, JMenu menu) {
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{
                        barra, menu, menu.getPopupMenu()
                });
            }
        });
    }

    @Override
    public void irARegistrarTrabajador() {
        setVisible(false);
        guiRegistrarTrabajador.mostrar();
    }

    @Override
    public void irAVerTrabajadores() {
        setVisible(false);
        guiVerTrabajadores.mostrar();
    }

    @Override
    public void irAEditarTrabajadores() {
        setVisible(false);
        guiEditarTrabajadores.mostrar();
    }

    @Override
    public void irAEliminarTrabajadores() {
        setVisible(false);
        guiEliminarTrabajadores.mostrar();
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