package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import controller.ControladorTrabajadores;
import controller.ResultadoOperacion;
import model.Doctor;
import model.Enfermero;
import model.NivelExperiencia;
import model.TrabajadorHospital;

/**
 * Editar Trabajadores: tabla tipo Excel con un botón "Editar" en cada
 * fila. El formulario de edición envía los datos en bruto al
 * controlador, que valida y decide si el cambio es aceptado.
 */
public class GUIEditarTrabajadores extends JFrame implements IGUIEditarTrabajadores {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private List<TrabajadorHospital> trabajadoresActuales;

    public GUIEditarTrabajadores(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Editar Trabajadores");
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

        JLabel titulo = new JLabel("Editar Trabajadores");
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
                BotonAccionTabla.crearRenderer("Editar", new Color(25, 118, 210)));
        tabla.getColumnModel().getColumn(columnaAcciones).setCellEditor(
                BotonAccionTabla.crearEditor(modeloTabla, "Editar", new Color(25, 118, 210), this::editarTrabajador));

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
    public void editarTrabajador(String idTrabajador) {
        TrabajadorHospital existente = controlador.buscarTrabajadorPorId(idTrabajador);
        if (existente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el trabajador.");
            return;
        }
        abrirFormularioEdicion(existente);
    }

    private void abrirFormularioEdicion(TrabajadorHospital existente) {
        JDialog dialogo = new JDialog(this, "Editar Trabajador", true);
        dialogo.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(15, 15, 5, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField campoId = new JTextField(existente.getIdTrabajador(), 18);
        campoId.setEditable(false);
        JTextField campoNombre = new JTextField(existente.getNombreCompleto(), 18);

        JComboBox<String> comboRol = new JComboBox<>(new String[]{"Doctor", "Enfermero"});
        comboRol.setEnabled(false); // el rol no cambia una vez creado el trabajador

        CardLayout cardLayout = new CardLayout();
        JPanel panelDetalleDinamico = new JPanel(cardLayout);

        JTextField campoEspecialidad = new JTextField(18);
        JPanel panelDoctor = new JPanel(new GridBagLayout());
        panelDoctor.setBorder(BorderFactory.createTitledBorder("Datos de Doctor"));
        GridBagConstraints gbcD = new GridBagConstraints();
        gbcD.insets = new Insets(4, 4, 4, 4);
        gbcD.fill = GridBagConstraints.HORIZONTAL;
        gbcD.gridx = 0; gbcD.gridy = 0;
        panelDoctor.add(new JLabel("Especialidad:"), gbcD);
        gbcD.gridx = 1; gbcD.weightx = 1;
        panelDoctor.add(campoEspecialidad, gbcD);

        JComboBox<NivelExperiencia> comboNivel = new JComboBox<>(NivelExperiencia.values());
        comboNivel.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NivelExperiencia) {
                    setText(TrabajadorTableUtil.etiquetaNivel((NivelExperiencia) value));
                }
                return c;
            }
        });
        JPanel panelEnfermero = new JPanel(new GridBagLayout());
        panelEnfermero.setBorder(BorderFactory.createTitledBorder("Datos de Enfermero"));
        GridBagConstraints gbcE = new GridBagConstraints();
        gbcE.insets = new Insets(4, 4, 4, 4);
        gbcE.fill = GridBagConstraints.HORIZONTAL;
        gbcE.gridx = 0; gbcE.gridy = 0;
        panelEnfermero.add(new JLabel("Nivel de experiencia:"), gbcE);
        gbcE.gridx = 1; gbcE.weightx = 1;
        panelEnfermero.add(comboNivel, gbcE);

        panelDetalleDinamico.add(panelDoctor, "DOCTOR");
        panelDetalleDinamico.add(panelEnfermero, "ENFERMERO");

        if (existente instanceof Doctor) {
            comboRol.setSelectedItem("Doctor");
            campoEspecialidad.setText(((Doctor) existente).getEspecialidad());
            cardLayout.show(panelDetalleDinamico, "DOCTOR");
        } else if (existente instanceof Enfermero) {
            comboRol.setSelectedItem("Enfermero");
            comboNivel.setSelectedItem(((Enfermero) existente).getNivelExperiencia());
            cardLayout.show(panelDetalleDinamico, "ENFERMERO");
        }

        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("ID del Trabajador:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoId, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Nombre Completo:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoNombre, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Rol:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(comboRol, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2; gbc.weightx = 1;
        panelForm.add(panelDetalleDinamico, gbc);
        gbc.gridwidth = 1;

        JLabel lblError = new JLabel(" ");
        lblError.setForeground(new Color(198, 40, 40));
        lblError.setBorder(new EmptyBorder(0, 15, 5, 15));

        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.setBackground(new Color(46, 125, 50));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setOpaque(true);
        btnGuardar.setBorderPainted(false);

        btnCancelar.addActionListener(e -> dialogo.dispose());
        btnGuardar.addActionListener(e -> {
            String rol = (String) comboRol.getSelectedItem();
            ResultadoOperacion resultado = controlador.editarTrabajador(
                    campoId.getText(), campoNombre.getText(), rol,
                    campoEspecialidad.getText(), (NivelExperiencia) comboNivel.getSelectedItem());

            if (resultado.isExito()) {
                dialogo.dispose();
                refrescarTabla();
                JOptionPane.showMessageDialog(this, resultado.getMensaje());
            } else {
                lblError.setText("<html><body style='width: 320px'>" + resultado.getMensaje() + "</body></html>");
            }
        });

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(lblError, BorderLayout.NORTH);
        panelSur.add(panelBotones, BorderLayout.SOUTH);

        dialogo.add(panelForm, BorderLayout.CENTER);
        dialogo.add(panelSur, BorderLayout.SOUTH);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}