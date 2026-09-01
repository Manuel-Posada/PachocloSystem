package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import controller.ControladorTrabajadores;
import model.TrabajadorHospital;

//ver y/o buscar trabajadores
public class GUIVerTrabajadores extends JFrame implements IGUIVerTrabajadores {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    private JTextField campoBusqueda;
    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private List<TrabajadorHospital> trabajadoresMostrados;

    public GUIVerTrabajadores(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Ver / Buscar Trabajadores");
        setSize(820, 500);
        setMinimumSize(new Dimension(620, 380));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @Override
    public void mostrar() {
        mostrarTodos();
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panelRaiz = new JPanel(new BorderLayout(0, 10));
        panelRaiz.setBorder(new EmptyBorder(15, 15, 15, 15));

        panelRaiz.add(construirPanelSuperior(), BorderLayout.NORTH);
        panelRaiz.add(construirPanelTabla(), BorderLayout.CENTER);
        panelRaiz.add(construirPanelInferior(), BorderLayout.SOUTH);

        setContentPane(panelRaiz);
        mostrarTodos();
        revalidate();
        repaint();
    }

    private JPanel construirPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));

        JLabel titulo = new JLabel("Trabajadores del Hospital");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        campoBusqueda = new JTextField(14);
        JButton btnBuscar = new JButton("Buscar por ID");
        JButton btnMostrarTodos = new JButton("Mostrar Todos");
        btnBuscar.addActionListener(e -> buscarPorId());
        btnMostrarTodos.addActionListener(e -> mostrarTodos());
        campoBusqueda.addActionListener(e -> buscarPorId());

        panelBusqueda.add(new JLabel("ID:"));
        panelBusqueda.add(campoBusqueda);
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(btnMostrarTodos);

        panel.add(titulo, BorderLayout.WEST);
        panel.add(panelBusqueda, BorderLayout.EAST);
        return panel;
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        lblEstado = new JLabel("No hay trabajadores", SwingConstants.CENTER);
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.ITALIC, 14f));
        lblEstado.setForeground(new Color(120, 120, 120));

        modeloTabla = TrabajadorTableUtil.crearModelo(false);
        tabla = new JTable(modeloTabla);
        TrabajadorTableUtil.estilizarTabla(tabla, false);

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetalle();
                }
            }
        });

        panel.add(lblEstado, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVolver = new JButton("Volver");
        btnVolver.addActionListener(e -> volver());
        panel.add(btnVolver);
        return panel;
    }

    @Override
    public void mostrarTodos() {
        campoBusqueda.setText("");
        trabajadoresMostrados = controlador.listarTrabajadores();
        TrabajadorTableUtil.poblarFilas(modeloTabla, trabajadoresMostrados, false);
        lblEstado.setText(trabajadoresMostrados.isEmpty()
                ? "No hay trabajadores"
                : "Total de trabajadores: " + trabajadoresMostrados.size());
    }

    @Override
    public void buscarPorId() {
        String idBuscado = campoBusqueda.getText();
        TrabajadorHospital encontrado = controlador.buscarTrabajadorPorId(idBuscado);

        if (encontrado == null) {
            trabajadoresMostrados = Collections.emptyList();
            TrabajadorTableUtil.poblarFilas(modeloTabla, trabajadoresMostrados, false);
            lblEstado.setText("No se encontró ningún trabajador con ID \""
                    + (idBuscado == null ? "" : idBuscado.trim()) + "\".");
            return;
        }

        trabajadoresMostrados = Collections.singletonList(encontrado);
        TrabajadorTableUtil.poblarFilas(modeloTabla, trabajadoresMostrados, false);
        lblEstado.setText("1 trabajador encontrado.");
    }

    private void verDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || trabajadoresMostrados == null || fila >= trabajadoresMostrados.size()) {
            return;
        }
        TrabajadorHospital t = trabajadoresMostrados.get(fila);
        String detalle = "ID: " + t.getIdTrabajador() + "\n"
                + "Nombre: " + t.getNombreCompleto() + "\n"
                + t.obtenerPerfil();
        JOptionPane.showMessageDialog(this, detalle, "Detalle del Trabajador", JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}