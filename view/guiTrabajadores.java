// GUITrabajadores.java
package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

import controller.ControladorTrabajadores;
import model.Doctor;
import model.Enfermero;
import model.NivelExperiencia;
import model.TrabajadorHospital;

public class GUITrabajadores extends JFrame implements IGUITrabajadores {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private List<TrabajadorHospital> trabajadoresActuales;

    private static final String[] COLUMNAS = {
            "ID", "Nombre Completo", "Rol", "Detalle", "Acciones"
    };
    private static final int COLUMNA_ACCIONES = 4;

    private static final Pattern PATRON_ID =
            Pattern.compile("^[A-Za-z0-9\\-]{1,20}$");
    private static final Pattern PATRON_NOMBRE =
            Pattern.compile("^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,59}$");
    private static final Pattern PATRON_CONTIENE_TEXTO =
            Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");

    public GUITrabajadores(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Gestión de Trabajadores");
        setSize(900, 520);
        setMinimumSize(new Dimension(700, 400));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    @Override
    public void mostrar() {
        refrescarTabla();
        setVisible(true);
    }

    //boton + tabla

    @Override
    public void mostrarOpciones() {
        JPanel panelRaiz = new JPanel(new BorderLayout(0, 10));
        panelRaiz.setBorder(new EmptyBorder(15, 15, 15, 15));

        panelRaiz.add(construirPanelSuperior(), BorderLayout.NORTH);
        panelRaiz.add(construirPanelTabla(), BorderLayout.CENTER);
        panelRaiz.add(construirPanelInferior(), BorderLayout.SOUTH);

        setContentPane(panelRaiz);
        refrescarTabla();
        revalidate();
        repaint();
    }

    private JPanel construirPanelSuperior() {
        JPanel panelSuperior = new JPanel(new BorderLayout());

        JLabel titulo = new JLabel("Trabajadores del Hospital");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JButton btnRegistrar = new JButton("+ Registrar Trabajador");
        btnRegistrar.setFont(btnRegistrar.getFont().deriveFont(Font.BOLD));
        btnRegistrar.setBackground(new Color(46, 125, 50));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setOpaque(true);
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnRegistrar.addActionListener(e -> registrarTrabajador());

        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(btnRegistrar, BorderLayout.EAST);
        return panelSuperior;
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        lblEstado = new JLabel("No hay trabajadores", SwingConstants.CENTER);
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.ITALIC, 14f));
        lblEstado.setForeground(new Color(120, 120, 120));

        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COLUMNA_ACCIONES;
            }
        };
        tabla = new JTable(modeloTabla);
        estilizarTabla();

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

    //apariencia tipo excel
    private void estilizarTabla() {
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(210, 210, 210));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setRowHeight(34);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new Color(204, 228, 247));
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(235, 235, 235));
        tabla.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
                }
                setBorder(new EmptyBorder(2, 8, 2, 8));
                return c;
            }
        };
        for (int i = 0; i < tabla.getColumnCount() - 1; i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        tabla.getColumnModel().getColumn(0).setPreferredWidth(90);   //ID
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);  //nombre
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);  //rol
        tabla.getColumnModel().getColumn(3).setPreferredWidth(260);  //detalle
        tabla.getColumnModel().getColumn(4).setPreferredWidth(170);  //acciones

        //botones Editar/Eliminar dentro de la propia celda de la tabla
        tabla.getColumnModel().getColumn(COLUMNA_ACCIONES).setCellRenderer(new PanelAccionesRenderer());
        tabla.getColumnModel().getColumn(COLUMNA_ACCIONES).setCellEditor(new PanelAccionesEditor());
    }

    private void refrescarTabla() {
        trabajadoresActuales = controlador.listarTrabajadores();
        modeloTabla.setRowCount(0);

        for (TrabajadorHospital t : trabajadoresActuales) {
            modeloTabla.addRow(new Object[]{
                    t.getIdTrabajador(),
                    t.getNombreCompleto(),
                    etiquetaRol(t),
                    detalleRol(t),
                    ""
            });
        }

        boolean vacio = trabajadoresActuales.isEmpty();
        lblEstado.setText(vacio ? "No hay trabajadores" : "Total de trabajadores: " + trabajadoresActuales.size());
    }

    private static String etiquetaRol(TrabajadorHospital t) {
        if (t instanceof Doctor) return "Doctor";
        if (t instanceof Enfermero) return "Enfermero";
        return "Otro";
    }

    private static String detalleRol(TrabajadorHospital t) {
        if (t instanceof Doctor) {
            return "Especialidad: " + ((Doctor) t).getEspecialidad();
        }
        if (t instanceof Enfermero) {
            return "Nivel: " + etiquetaNivel(((Enfermero) t).getNivelExperiencia());
        }
        return "";
    }

    private static String etiquetaNivel(NivelExperiencia nivel) {
        if (nivel == null) return "";
        switch (nivel) {
            case NOVATO: return "Novato";
            case PRINCIPIANTE: return "Principiante";
            case AVANZADO: return "Avanzado";
            default: return nivel.toString();
        }
    }


    private JPanel construirPanelBotones(JButton btnEditar, JButton btnEliminar) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        panel.setOpaque(true);

        btnEditar.setFont(btnEditar.getFont().deriveFont(Font.BOLD, 11f));
        btnEditar.setBackground(new Color(25, 118, 210));
        btnEditar.setForeground(Color.WHITE);
        btnEditar.setFocusPainted(false);
        btnEditar.setOpaque(true);
        btnEditar.setBorderPainted(false);
        btnEditar.setMargin(new Insets(2, 8, 2, 8));

        btnEliminar.setFont(btnEliminar.getFont().deriveFont(Font.BOLD, 11f));
        btnEliminar.setBackground(new Color(198, 40, 40));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.setOpaque(true);
        btnEliminar.setBorderPainted(false);
        btnEliminar.setMargin(new Insets(2, 8, 2, 8));

        panel.add(btnEditar);
        panel.add(btnEliminar);
        return panel;
    }

    private class PanelAccionesRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JButton btnEditar = new JButton("Editar");
            JButton btnEliminar = new JButton("Eliminar");
            JPanel panel = construirPanelBotones(btnEditar, btnEliminar);
            panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
            return panel;
        }
    }

    private class PanelAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int filaActual;

        PanelAccionesEditor() {
            JButton btnEditar = new JButton("Editar");
            JButton btnEliminar = new JButton("Eliminar");
            panel = construirPanelBotones(btnEditar, btnEliminar);

            btnEditar.addActionListener(e -> {
                String id = String.valueOf(modeloTabla.getValueAt(filaActual, 0));
                fireEditingStopped();
                editarTrabajador(id);
            });
            btnEliminar.addActionListener(e -> {
                String id = String.valueOf(modeloTabla.getValueAt(filaActual, 0));
                fireEditingStopped();
                eliminarTrabajador(id);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            filaActual = row;
            panel.setBackground(new Color(204, 228, 247));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    @Override
    public void registrarTrabajador() {
        abrirFormularioTrabajador(null);
    }

    @Override
    public void editarTrabajador(String idTrabajador) {
        TrabajadorHospital existente = controlador.buscarTrabajadorPorId(idTrabajador);
        if (existente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el trabajador.");
            return;
        }
        abrirFormularioTrabajador(existente);
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

        boolean ok = controlador.eliminarTrabajador(idTrabajador);
        refrescarTabla();
        JOptionPane.showMessageDialog(this, ok ? "Trabajador eliminado." : "No se encontró el trabajador.");
    }

    private void abrirFormularioTrabajador(TrabajadorHospital existente) {
        boolean modoEdicion = existente != null;

        JDialog dialogo = new JDialog(this, modoEdicion ? "Editar Trabajador" : "Registrar Trabajador", true);
        dialogo.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(15, 15, 5, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField campoId = new JTextField(18);
        JTextField campoNombre = new JTextField(18);
        JComboBox<String> comboRol = new JComboBox<>(new String[]{"Doctor", "Enfermero"});

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
                    setText(etiquetaNivel((NivelExperiencia) value));
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

        if (modoEdicion) {
            campoId.setText(existente.getIdTrabajador());
            campoId.setEditable(false);
            campoNombre.setText(existente.getNombreCompleto());

            if (existente instanceof Doctor) {
                comboRol.setSelectedItem("Doctor");
                campoEspecialidad.setText(((Doctor) existente).getEspecialidad());
                cardLayout.show(panelDetalleDinamico, "DOCTOR");
            } else if (existente instanceof Enfermero) {
                comboRol.setSelectedItem("Enfermero");
                comboNivel.setSelectedItem(((Enfermero) existente).getNivelExperiencia());
                cardLayout.show(panelDetalleDinamico, "ENFERMERO");
            }
            //NO se puede cambiar el rol del trabajador
            comboRol.setEnabled(false);
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
            String resultado = validarYGuardarTrabajador(
                    campoId.getText(), campoNombre.getText(),
                    (String) comboRol.getSelectedItem(),
                    campoEspecialidad.getText(),
                    (NivelExperiencia) comboNivel.getSelectedItem(),
                    modoEdicion, modoEdicion ? existente.getIdTrabajador() : null);

            if (resultado == null) {
                dialogo.dispose();
                refrescarTabla();
                JOptionPane.showMessageDialog(this, modoEdicion
                        ? "Trabajador actualizado correctamente."
                        : "Trabajador registrado correctamente.");
            } else {
                lblError.setText("<html><body style='width: 320px'>" + resultado + "</body></html>");
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

    //aqui se verifica lo que ingresa el usuario al formulario
    private String validarYGuardarTrabajador(String idTexto, String nombreTexto, String rolSeleccionado,
            String especialidadTexto, NivelExperiencia nivelSeleccionado,
            boolean modoEdicion, String idOriginal) {

        StringBuilder errores = new StringBuilder();

        String id = idTexto == null ? "" : idTexto.trim();
        String nombre = nombreTexto == null ? "" : nombreTexto.trim();

        if (id.isEmpty()) {
            errores.append("• El ID del trabajador es obligatorio.<br>");
        } else if (!PATRON_ID.matcher(id).matches()) {
            errores.append("• El ID solo puede tener letras, números y guiones (sin espacios).<br>");
        }

        if (nombre.isEmpty()) {
            errores.append("• El nombre completo es obligatorio.<br>");
        } else if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            errores.append("• El nombre debe tener solo letras y espacios (3 a 60 caracteres).<br>");
        }

        if (rolSeleccionado == null) {
            errores.append("• Debe seleccionar un rol.<br>");
        }

        TrabajadorHospital rolEspecifico = null;

        if ("Doctor".equals(rolSeleccionado)) {
            String especialidad = especialidadTexto == null ? "" : especialidadTexto.trim();
            if (especialidad.isEmpty()) {
                errores.append("• La especialidad es obligatoria.<br>");
            } else if (especialidad.length() < 3 || !PATRON_CONTIENE_TEXTO.matcher(especialidad).matches()) {
                errores.append("• La especialidad debe ser un texto descriptivo (mínimo 3 caracteres).<br>");
            } else if (errores.length() == 0) {
                rolEspecifico = new Doctor(id, nombre, especialidad);
            }
        } else if ("Enfermero".equals(rolSeleccionado)) {
            if (nivelSeleccionado == null) {
                errores.append("• Debe seleccionar un nivel de experiencia.<br>");
            } else if (errores.length() == 0) {
                rolEspecifico = new Enfermero(id, nombre, nivelSeleccionado);
            }
        }

        if (errores.length() > 0) {
            return errores.toString();
        }

        boolean ok = modoEdicion
                ? controlador.editarTrabajador(id, nombre, rolEspecifico)
                : controlador.registrarTrabajador(id, nombre, rolEspecifico);

        if (!ok) {
            return modoEdicion
                    ? "• No se pudo actualizar el trabajador."
                    : "• No se pudo registrar: verifique que el ID no exista ya.";
        }

        return null; //todo salio 100/10 si se llega a este retorno
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}