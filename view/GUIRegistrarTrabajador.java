package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import controller.ControladorTrabajadores;
import controller.ResultadoOperacion;
import model.NivelExperiencia;

public class GUIRegistrarTrabajador extends JFrame implements IGUIRegistrarTrabajador {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    private JTextField campoId;
    private JTextField campoNombre;
    private JComboBox<String> comboRol;
    private JTextField campoEspecialidad;
    private JComboBox<NivelExperiencia> comboNivel;
    private CardLayout cardLayout;
    private JPanel panelDetalleDinamico;
    private JLabel lblMensaje;

    public GUIRegistrarTrabajador(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Registrar Trabajador");
        setSize(480, 430);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    @Override
    public void mostrar() {
        limpiarFormulario();
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panelRaiz = new JPanel(new BorderLayout(0, 10));
        panelRaiz.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("Registrar Nuevo Trabajador");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        panelRaiz.add(titulo, BorderLayout.NORTH);

        panelRaiz.add(construirFormulario(), BorderLayout.CENTER);
        panelRaiz.add(construirPanelInferior(), BorderLayout.SOUTH);

        setContentPane(panelRaiz);
        revalidate();
        repaint();
    }

    private JPanel construirFormulario() {
        JPanel panelForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        campoId = new JTextField(18);
        campoNombre = new JTextField(18);
        comboRol = new JComboBox<>(new String[]{"Doctor", "Enfermero"});

        cardLayout = new CardLayout();
        panelDetalleDinamico = new JPanel(cardLayout);

        campoEspecialidad = new JTextField(18);
        JPanel panelDoctor = new JPanel(new GridBagLayout());
        panelDoctor.setBorder(BorderFactory.createTitledBorder("Datos de Doctor"));
        GridBagConstraints gbcD = new GridBagConstraints();
        gbcD.insets = new Insets(4, 4, 4, 4);
        gbcD.fill = GridBagConstraints.HORIZONTAL;
        gbcD.gridx = 0; gbcD.gridy = 0;
        panelDoctor.add(new JLabel("Especialidad:"), gbcD);
        gbcD.gridx = 1; gbcD.weightx = 1;
        panelDoctor.add(campoEspecialidad, gbcD);

        comboNivel = new JComboBox<>(NivelExperiencia.values());
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
        comboRol.addActionListener(e ->
                cardLayout.show(panelDetalleDinamico, "Doctor".equals(comboRol.getSelectedItem()) ? "DOCTOR" : "ENFERMERO"));

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

        fila++;
        lblMensaje = new JLabel(" ");
        lblMensaje.setBorder(new EmptyBorder(6, 0, 0, 0));
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        panelForm.add(lblMensaje, gbc);

        return panelForm;
    }

    private JPanel construirPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVolver = new JButton("Volver");
        JButton btnRegistrar = new JButton("Registrar");
        btnRegistrar.setBackground(new Color(46, 125, 50));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setOpaque(true);
        btnRegistrar.setBorderPainted(false);

        btnVolver.addActionListener(e -> volver());
        btnRegistrar.addActionListener(e -> registrarTrabajador());

        panel.add(btnVolver);
        panel.add(btnRegistrar);
        return panel;
    }

    @Override
    public void registrarTrabajador() {
        String rol = (String) comboRol.getSelectedItem();
        ResultadoOperacion resultado = controlador.registrarTrabajador(
                campoId.getText(), campoNombre.getText(), rol,
                campoEspecialidad.getText(), (NivelExperiencia) comboNivel.getSelectedItem());

        mostrarResultado(resultado);
        if (resultado.isExito()) {
            limpiarFormulario();
        }
    }

    private void mostrarResultado(ResultadoOperacion resultado) {
        lblMensaje.setForeground(resultado.isExito() ? new Color(46, 125, 50) : new Color(198, 40, 40));
        lblMensaje.setText("<html><body style='width: 340px'>" + resultado.getMensaje() + "</body></html>");
    }

    private void limpiarFormulario() {
        campoId.setText("");
        campoNombre.setText("");
        campoEspecialidad.setText("");
        comboRol.setSelectedIndex(0);
        cardLayout.show(panelDetalleDinamico, "DOCTOR");
        lblMensaje.setText(" ");
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}