package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import controller.ControladorTrabajadores;
import controller.ResultadoOperacion;
import model.TrabajadorHospital;

public class GUIEliminarTrabajadores extends JFrame implements IGUIEliminarTrabajadores {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private List<TrabajadorHospital> trabajadoresActuales;

    public GUIEliminarTrabajadores(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Eliminar Trabajadores");
        setSize(820, 500);
        setMinimumSize(new Dimension(620, 380));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @Override
    public void mostrar() {
        refrescarTabla();
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panelRaiz = new JPanel(new BorderLayout(0, 10));
        panelRaiz.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Eliminar Trabajadores");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        panelRaiz.add(titulo, BorderLayout.NORTH);
        panelRaiz.add(construirPanelTabla(), BorderLayout.CENTER);
        panelRaiz.add(construirPanelInferior(), BorderLayout.SOUTH);

        setContentPane(panelRaiz);
        refrescarTabla();
        revalidate();
        repaint();
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        lblEstado = new JLabel("No hay trabajadores", SwingConstants.CENTER);
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.ITALIC, 14f));
        lblEstado.setForeground(new Color(120, 120, 120));

        modeloTabla = TrabajadorTableUtil.crearModelo(true);
        tabla = new JTable(modeloTabla);
        TrabajadorTableUtil.estilizarTabla(tabla, true);

        int columnaAcciones = tabla.getColumnCount() - 1;
        tabla.getColumnModel().getColumn(columnaAcciones).setCellRenderer(
                BotonAccionTabla.crearRenderer("Eliminar", new Color(198, 40, 40)));
        tabla.getColumnModel().getColumn(columnaAcciones).setCellEditor(
                BotonAccionTabla.crearEditor(modeloTabla, "Eliminar", new Color(198, 40, 40), this::eliminarTrabajador));

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

    private void refrescarTabla() {
        trabajadoresActuales = controlador.listarTrabajadores();
        TrabajadorTableUtil.poblarFilas(modeloTabla, trabajadoresActuales, true);
        lblEstado.setText(trabajadoresActuales.isEmpty()
                ? "No hay trabajadores"
                : "Total de trabajadores: " + trabajadoresActuales.size());
    }

    @Override
    public void eliminarTrabajador(String idTrabajador) {
        TrabajadorHospital existente = controlador.buscarTrabajadorPorId(idTrabajador);
        String nombre = existente != null ? existente.getNombreCompleto() : idTrabajador;

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar al trabajador \"" + nombre + "\" (ID: " + idTrabajador + ")?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        ResultadoOperacion resultado = controlador.eliminarTrabajador(idTrabajador);
        refrescarTabla();
        JOptionPane.showMessageDialog(this, resultado.getMensaje());
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}