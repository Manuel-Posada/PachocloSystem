package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import controller.ControladorPaciente;
import model.Paciente;

public class GUIPacientes extends JFrame implements IGUIPacientes {

    // --- 1. VARIABLES DE CLASE ---
    private IGUIPrincipal guiPrincipal;
    private IGUIHistorialClinico guiHistorial; // <- Referencia a la ventana de historial
    private final ControladorPaciente controlador;

    // Componentes gráficos
    private JLabel lblEstado;
    private JTextField campoBusqueda;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    
    // Estado actual
    private List<Paciente> pacientesActuales;

    // Constantes
    private static final String[] COLUMNAS = {
            "ID", "Nombre", "Edad", "Habitación", "Registros Clínicos", "Acciones"
    };
    private static final int COLUMNA_ACCIONES = 5;

    // Validaciones (Regex)
    private static final Pattern PATRON_ID = Pattern.compile("^[A-Za-z0-9\\-]{1,20}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,59}$");
    private static final Pattern PATRON_ENTERO = Pattern.compile("^\\d{1,3}$");

    // --- 2. CONSTRUCTOR Y CONFIGURACIÓN ---
    public GUIPacientes(ControladorPaciente controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    public void setGuiHistorial(IGUIHistorialClinico guiHistorial) {
        this.guiHistorial = guiHistorial;
    }

    private void configurarVentana() {
        setTitle("Gestión de Pacientes");
        setSize(950, 520);
        setMinimumSize(new Dimension(800, 400));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    // --- 3. MÉTODOS DE VISUALIZACIÓN ---
    @Override
    public void mostrar() {
        refrescarTabla();
        setVisible(true);
    }

    @Override
    public void volver() {
        setVisible(false);
        if (guiPrincipal != null) {
            guiPrincipal.mostrar();
        }
    }

    // --- 4. CONSTRUCCIÓN DE LA INTERFAZ (PANELES) ---
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
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("Pacientes del Hospital");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        campoBusqueda = new JTextField();
        campoBusqueda.setToolTipText("Buscar por ID o nombre...");
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refrescarTabla(); }
            @Override public void removeUpdate(DocumentEvent e) { refrescarTabla(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescarTabla(); }
        });
        JPanel panelBusqueda = new JPanel(new BorderLayout(6, 0));
        panelBusqueda.add(new JLabel("Buscar:"), BorderLayout.WEST);
        panelBusqueda.add(campoBusqueda, BorderLayout.CENTER);
        panelBusqueda.setPreferredSize(new Dimension(260, panelBusqueda.getPreferredSize().height));

        JButton btnRegistrar = new JButton("+ Registrar Paciente");
        btnRegistrar.setFont(btnRegistrar.getFont().deriveFont(Font.BOLD));
        btnRegistrar.setBackground(new Color(46, 125, 50));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFocusPainted(false);
        btnRegistrar.setOpaque(true);
        btnRegistrar.setBorderPainted(false);
        btnRegistrar.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnRegistrar.addActionListener(e -> registrarPaciente());

        JPanel panelDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelDerecha.add(panelBusqueda);
        panelDerecha.add(btnRegistrar);

        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(panelDerecha, BorderLayout.EAST);
        return panelSuperior;
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        lblEstado = new JLabel("No hay pacientes", SwingConstants.CENTER);
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.ITALIC, 14f));
        lblEstado.setForeground(new Color(120, 120, 120));

        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COLUMNA_ACCIONES; // Solo la columna de botones es "editable" (clickeable)
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

    // --- 5. LÓGICA DE TABLA Y BOTONES (RENDER / EDITOR) ---
    private void estilizarTabla() {
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(210, 210, 210));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setRowHeight(36);
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

        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);   //ID
        tabla.getColumnModel().getColumn(1).setPreferredWidth(180);  //Nombre
        tabla.getColumnModel().getColumn(2).setPreferredWidth(60);   //Edad
        tabla.getColumnModel().getColumn(3).setPreferredWidth(90);   //Habitación
        tabla.getColumnModel().getColumn(4).setPreferredWidth(140);  //Registros clínicos
        tabla.getColumnModel().getColumn(5).setPreferredWidth(260);  //Acciones (Aumentado para 3 botones)

        tabla.getColumnModel().getColumn(COLUMNA_ACCIONES).setCellRenderer(new PanelAccionesRenderer());
        tabla.getColumnModel().getColumn(COLUMNA_ACCIONES).setCellEditor(new PanelAccionesEditor());
    }

    private void refrescarTabla() {
        List<Paciente> todos = controlador.listarPacientes();
        String filtro = campoBusqueda == null ? "" : campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);

        if (filtro.isEmpty()) {
            pacientesActuales = todos;
        } else {
            pacientesActuales = new ArrayList<>();
            for (Paciente p : todos) {
                if (p.getIdPaciente().toLowerCase(Locale.ROOT).contains(filtro)
                        || p.getNombre().toLowerCase(Locale.ROOT).contains(filtro)) {
                    pacientesActuales.add(p);
                }
            }
        }

        modeloTabla.setRowCount(0);
        for (Paciente p : pacientesActuales) {
            modeloTabla.addRow(new Object[]{
                    p.getIdPaciente(),
                    p.getNombre(),
                    p.getEdad(),
                    p.getHabitacion(),
                    p.obtenerHistorial().size(),
                    "" // Se llena visualmente con los botones del Renderer
            });
        }

        if (todos.isEmpty()) {
            lblEstado.setText("No hay pacientes");
        } else if (pacientesActuales.isEmpty()) {
            lblEstado.setText("Sin resultados para \"" + campoBusqueda.getText().trim() + "\"");
        } else {
            lblEstado.setText("Total de pacientes: " + pacientesActuales.size()
                    + (filtro.isEmpty() ? "" : " de " + todos.size()));
        }
    }

    private JPanel construirPanelBotones(JButton btnHistorial, JButton btnEditar, JButton btnEliminar) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        panel.setOpaque(true);

        JButton[] botones = {btnHistorial, btnEditar, btnEliminar};
        Color[] colores = {new Color(46, 125, 50), new Color(25, 118, 210), new Color(198, 40, 40)};

        for (int i = 0; i < botones.length; i++) {
            botones[i].setFont(botones[i].getFont().deriveFont(Font.BOLD, 11f));
            botones[i].setBackground(colores[i]);
            botones[i].setForeground(Color.WHITE);
            botones[i].setFocusPainted(false);
            botones[i].setOpaque(true);
            botones[i].setBorderPainted(false);
            botones[i].setMargin(new Insets(2, 6, 2, 6));
            panel.add(botones[i]);
        }
        return panel;
    }

    private class PanelAccionesRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JButton btnHistorial = new JButton("Historial");
            JButton btnEditar = new JButton("Editar");
            JButton btnEliminar = new JButton("Eliminar");
            
            JPanel panel = construirPanelBotones(btnHistorial, btnEditar, btnEliminar);
            panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
            return panel;
        }
    }

    private class PanelAccionesEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private int filaActual;

        PanelAccionesEditor() {
            JButton btnHistorial = new JButton("Historial");
            JButton btnEditar = new JButton("Editar");
            JButton btnEliminar = new JButton("Eliminar");
            
            panel = construirPanelBotones(btnHistorial, btnEditar, btnEliminar);

            btnHistorial.addActionListener(e -> {
                String id = String.valueOf(modeloTabla.getValueAt(filaActual, 0));
                String nombre = String.valueOf(modeloTabla.getValueAt(filaActual, 1));
                fireEditingStopped(); // Detener edición antes de abrir nueva ventana
                
                if (guiHistorial != null) {
                    guiHistorial.mostrar(id, nombre);
                } else {
                    JOptionPane.showMessageDialog(panel, "Error: El módulo de Historial Clínico no está enlazado.",
                            "Módulo no encontrado", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnEditar.addActionListener(e -> {
                String id = String.valueOf(modeloTabla.getValueAt(filaActual, 0));
                fireEditingStopped();
                editarPaciente(id);
            });

            btnEliminar.addActionListener(e -> {
                String id = String.valueOf(modeloTabla.getValueAt(filaActual, 0));
                fireEditingStopped();
                eliminarPaciente(id);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            filaActual = row;
            panel.setBackground(new Color(204, 228, 247)); // Fondo al seleccionar
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    // --- 6. ACCIONES CRUD DE PACIENTES ---
    @Override
    public void registrarPaciente() {
        abrirFormularioPaciente(null);
    }

    @Override
    public void editarPaciente(String idPaciente) {
        Paciente existente = buscarEnListaActual(idPaciente);
        if (existente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el paciente.");
            return;
        }
        abrirFormularioPaciente(existente);
    }

    private Paciente buscarEnListaActual(String id) {
        for (Paciente p : controlador.listarPacientes()) {
            if (p.getIdPaciente().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void eliminarPaciente(String idPaciente) {
        Paciente existente = buscarEnListaActual(idPaciente);
        String nombre = existente != null ? existente.getNombre() : idPaciente;

        int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar al paciente \"" + nombre + "\" (ID: " + idPaciente + ")?"
                        + (existente != null && !existente.obtenerHistorial().isEmpty()
                                ? "\nEsto también eliminará sus " + existente.obtenerHistorial().size() + " registro(s) clínico(s)."
                                : ""),
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        boolean ok = controlador.eliminarPaciente(idPaciente);
        refrescarTabla();
        JOptionPane.showMessageDialog(this, ok ? "Paciente eliminado." : "No se encontró el paciente.");
    }

    private void abrirFormularioPaciente(Paciente existente) {
        boolean modoEdicion = existente != null;

        JDialog dialogo = new JDialog(this, modoEdicion ? "Editar Paciente" : "Registrar Paciente", true);
        dialogo.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(15, 15, 5, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField campoId = new JTextField(18);
        JTextField campoNombre = new JTextField(18);
        JTextField campoEdad = new JTextField(18);
        JTextField campoHabitacion = new JTextField(18);

        if (modoEdicion) {
            campoId.setText(existente.getIdPaciente());
            campoId.setEditable(false);
            campoNombre.setText(existente.getNombre());
            campoEdad.setText(String.valueOf(existente.getEdad()));
            campoHabitacion.setText(String.valueOf(existente.getHabitacion()));
        }

        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("ID del Paciente:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoId, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Nombre Completo:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoNombre, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Edad:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoEdad, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Número de Habitación:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoHabitacion, gbc);

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
            String resultado = validarYGuardarPaciente(
                    campoId.getText(), campoNombre.getText(),
                    campoEdad.getText(), campoHabitacion.getText(),
                    modoEdicion);

            if (resultado == null) {
                dialogo.dispose();
                refrescarTabla();
                JOptionPane.showMessageDialog(this, modoEdicion
                        ? "Paciente actualizado correctamente."
                        : "Paciente registrado correctamente.");
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

    // --- 7. VALIDACIONES ---
    private String validarYGuardarPaciente(String idTexto, String nombreTexto,
            String edadTexto, String habitacionTexto, boolean modoEdicion) {

        StringBuilder errores = new StringBuilder();

        String id = idTexto == null ? "" : idTexto.trim();
        String nombre = nombreTexto == null ? "" : nombreTexto.trim();

        if (id.isEmpty()) {
            errores.append("• El ID del paciente es obligatorio.<br>");
        } else if (!PATRON_ID.matcher(id).matches()) {
            errores.append("• El ID solo puede tener letras, números y guiones.<br>");
        }

        if (nombre.isEmpty()) {
            errores.append("• El nombre completo es obligatorio.<br>");
        } else if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            errores.append("• El nombre debe tener letras y espacios (3 a 60 caracteres).<br>");
        }

        Integer edad = validarEntero(edadTexto, 0, 120);
        if (edad == null) {
            errores.append("• La edad debe ser un número entero entre 0 y 120.<br>");
        }

        Integer habitacion = validarEntero(habitacionTexto, 1, 999);
        if (habitacion == null) {
            errores.append("• El número de habitación debe ser un entero entre 1 y 999.<br>");
        }

        if (errores.length() > 0) {
            return errores.toString();
        }

        boolean ok = modoEdicion
                ? controlador.editarPaciente(id, nombre, edad, habitacion)
                : controlador.registrarPaciente(id, nombre, edad, habitacion);

        if (!ok) {
            return modoEdicion
                    ? "• No se pudo actualizar el paciente."
                    : "• No se pudo registrar: verifique que el ID no exista ya.";
        }

        return null; // sin errores
    }

    private Integer validarEntero(String texto, int min, int max) {
        if (texto == null) return null;
        texto = texto.trim();
        if (!PATRON_ENTERO.matcher(texto).matches()) return null;
        int valor = Integer.parseInt(texto);
        if (valor < min || valor > max) return null;
        return valor;
    }
}