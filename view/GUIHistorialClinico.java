package view;

import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Function;

import controller.ControladorHistorialClinico;
import controller.ControladorTrabajadores;
import controller.ControladorPaciente;
import model.TipoRegistro;
import model.TrabajadorHospital;
import model.RegistroConPaciente;
import model.RegistroClinico;
import model.Paciente;

import java.util.function.BiFunction;

public class GUIHistorialClinico extends JFrame implements IGUIHistorialClinico {

    // --- 1. VARIABLES DE CLASE ---
    private IGUIPrincipal guiPrincipal;
    private final ControladorHistorialClinico controlador;
    private final ControladorTrabajadores controladorTrabajadores;
    private final ControladorPaciente controladorPaciente;

    // Componentes gráficos
    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private JTextField campoBusqueda;
    private JComboBox<String> comboFiltro;

    // Estado actual de la vista
    private List<RegistroConPaciente> registrosActuales;
    private String idPacienteActual;
    private String nombrePacienteActual;

    // Constantes
    private static final String[] COLUMNAS = {
            "ID Paciente", "Paciente", "Fecha y Hora", "Tipo", "Autor", "Contenido"
    };
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Validaciones (Regex)
    private static final Pattern PATRON_CONTIENE_TEXTO = Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");
    private static final Pattern PATRON_ENTERO = Pattern.compile("^\\d{1,4}$");
    private static final Pattern PATRON_DECIMAL = Pattern.compile("^\\d{1,3}(\\.\\d{1,2})?$");

    public GUIHistorialClinico(ControladorHistorialClinico controlador, ControladorTrabajadores controladorTrabajadores,
                                ControladorPaciente controladorPaciente) {
        this.controlador = controlador;
        this.controladorTrabajadores = controladorTrabajadores;
        this.controladorPaciente = controladorPaciente;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Historial Clínico");
        setSize(900, 520);
        setMinimumSize(new Dimension(700, 400));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    // --- 3. MÉTODOS DE VISUALIZACIÓN Y NAVEGACIÓN ---

    @Override
    public void mostrar() {
        this.idPacienteActual = null;
        this.nombrePacienteActual = null;
        setTitle("Historial Clínico General");
        comboFiltro.setVisible(true);
        comboFiltro.setSelectedItem("Todos");
        refrescarTabla();
        aplicarFiltro();
        setVisible(true);
    }

    @Override
    public void mostrar(String idPaciente, String nombrePaciente) {
        this.idPacienteActual = idPaciente;
        this.nombrePacienteActual = nombrePaciente;
        setTitle("Historial Clínico - " + nombrePaciente);
        comboFiltro.setVisible(false);
        refrescarTabla();
        aplicarFiltro();
        setVisible(true);
    }

    @Override
    public void volver() {
        setVisible(false);

        if (idPacienteActual != null) {
            // Se abrió desde la tabla de un paciente específico (GUIPacientes ya está detrás visible).
            // Solo hay que cerrar esta ventana, no tocar el menú principal.
            return;
        }

        // Se abrió desde el menú general -> ahí sí regresa al menú principal.
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
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));

        JLabel titulo = new JLabel("Registros Clínicos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        // --- Panel de búsqueda/filtro (centro) ---
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JLabel lblBuscar = new JLabel("Buscar:");
        campoBusqueda = new JTextField(15);

        comboFiltro = new JComboBox<>(new String[]{"Todos", "Por Paciente", "Por Autor"});

        campoBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
        });
        comboFiltro.addActionListener(e -> aplicarFiltro());

        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(campoBusqueda);
        panelBusqueda.add(comboFiltro);

        // Si estamos viendo el historial de UN paciente específico, ocultamos el combo:
        // no tiene sentido filtrar "por paciente" si ya se sabe cuál es.
        comboFiltro.setVisible(idPacienteActual == null);

        // --- Botón agregar (derecha) ---
        JButton btnAgregar = new JButton("+ Agregar Registro Clínico");
        btnAgregar.setFont(btnAgregar.getFont().deriveFont(Font.BOLD));
        btnAgregar.setBackground(new Color(46, 125, 50));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setOpaque(true);
        btnAgregar.setBorderPainted(false);
        btnAgregar.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnAgregar.addActionListener(e -> agregarRegistroClinico());

        panelSuperior.add(titulo, BorderLayout.WEST);
        panelSuperior.add(panelBusqueda, BorderLayout.CENTER);
        panelSuperior.add(btnAgregar, BorderLayout.EAST);
        return panelSuperior;
    }

    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        lblEstado = new JLabel("No hay registros", SwingConstants.CENTER);
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.ITALIC, 14f));
        lblEstado.setForeground(new Color(120, 120, 120));

        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        estilizarTabla();

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verHistorialClinico();
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

    private void estilizarTabla() {
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(210, 210, 210));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setRowHeight(26);
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

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        tabla.getColumnModel().getColumn(0).setPreferredWidth(90);   //ID paciente
        tabla.getColumnModel().getColumn(1).setPreferredWidth(140);  //paciente
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);  //fecha
        tabla.getColumnModel().getColumn(3).setPreferredWidth(110);  //tipo
        tabla.getColumnModel().getColumn(4).setPreferredWidth(140);  //autor
        tabla.getColumnModel().getColumn(5).setPreferredWidth(320);  //contenido
    }

    // --- 5. LÓGICA DE TABLA Y DETALLES ---

    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        registrosActuales = new ArrayList<>();

        if (idPacienteActual != null) {
            // Modo: Un solo paciente
            List<RegistroClinico> registrosPaciente = controlador.obtenerRegistrosPorPaciente(idPacienteActual);
            for (RegistroClinico r : registrosPaciente) {
                registrosActuales.add(new RegistroConPaciente(idPacienteActual, nombrePacienteActual, r));
            }
        } else {
            // Modo: Todos los pacientes
            registrosActuales = controlador.obtenerTodosLosRegistros();
        }

        for (RegistroConPaciente rc : registrosActuales) {
            RegistroClinico r = rc.getRegistro();
            modeloTabla.addRow(new Object[]{
                    rc.getIdPaciente(),
                    rc.getNombrePaciente(),
                    r.getFecha().format(FORMATO_FECHA),
                    etiquetaTipo(r.getTipo()),
                    r.getAutor().getNombreCompleto(),
                    r.getContenido()
            });
        }

        boolean vacio = registrosActuales.isEmpty();
        lblEstado.setText(vacio ? "No hay registros" : "Total de registros: " + registrosActuales.size());
    }

    private void aplicarFiltro() {
        if (registrosActuales == null) return;

        String texto = campoBusqueda.getText() == null ? "" : campoBusqueda.getText().trim().toLowerCase();
        String modo = (idPacienteActual != null) ? "Por Autor" : (String) comboFiltro.getSelectedItem();

        modeloTabla.setRowCount(0);
        int contador = 0;

        for (RegistroConPaciente rc : registrosActuales) {
            RegistroClinico r = rc.getRegistro();
            boolean coincide;

            boolean coincidePaciente = rc.getNombrePaciente().toLowerCase().contains(texto)
                    || rc.getIdPaciente().toLowerCase().contains(texto);
            boolean coincideAutor = r.getAutor().getNombreCompleto().toLowerCase().contains(texto)
                    || r.getAutor().getIdTrabajador().toLowerCase().contains(texto);

            if (texto.isEmpty()) {
                coincide = true;
            } else if ("Por Paciente".equals(modo)) {
                coincide = coincidePaciente;
            } else if ("Por Autor".equals(modo)) {
                coincide = coincideAutor;
            } else {
                coincide = coincidePaciente || coincideAutor;
            }

            if (coincide) {
                modeloTabla.addRow(new Object[]{
                        rc.getIdPaciente(),
                        rc.getNombrePaciente(),
                        r.getFecha().format(FORMATO_FECHA),
                        etiquetaTipo(r.getTipo()),
                        r.getAutor().getNombreCompleto(),
                        r.getContenido()
                });
                contador++;
            }
        }

        lblEstado.setText(contador == 0 ? "No hay registros que coincidan"
                : "Total de registros: " + contador);
    }

    @Override
    public void verHistorialClinico() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || registrosActuales == null || fila >= registrosActuales.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro de la tabla para ver su detalle.");
            return;
        }

        RegistroConPaciente rc = registrosActuales.get(fila);
        RegistroClinico r = rc.getRegistro();

        String detalle = "Paciente: " + rc.getNombrePaciente() + " (ID: " + rc.getIdPaciente() + ")\n"
                + "Fecha: " + r.getFecha().format(FORMATO_FECHA) + "\n"
                + "Tipo: " + etiquetaTipo(r.getTipo()) + "\n"
                + "Autor: " + r.getAutor().getNombreCompleto() + "\n\n"
                + r.getContenido();

        JTextArea areaDetalle = new JTextArea(detalle, 10, 40);
        areaDetalle.setEditable(false);
        areaDetalle.setLineWrap(true);
        areaDetalle.setWrapStyleWord(true);

        JOptionPane.showMessageDialog(this, new JScrollPane(areaDetalle),
                "Detalle del Registro Clínico", JOptionPane.PLAIN_MESSAGE);
    }

    private static String etiquetaTipo(TipoRegistro tipo) {
        switch (tipo) {
            case DIAGNOSTICO: return "Diagnóstico";
            case EVOLUCION: return "Evolución";
            case MEDICACION: return "Medicación";
            case SIGNOS_VITALES: return "Signos Vitales";
            default: return tipo.toString();
        }
    }

    // --- 6. FORMULARIO DE AGREGAR REGISTRO ---

    @Override
    public void agregarRegistroClinico() {
        JDialog dialogo = new JDialog(this, "Agregar Registro Clínico", true);
        dialogo.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(15, 15, 5, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Campo de Paciente ---
        // Si estamos viendo el historial de un paciente específico, se bloquea
        // (ya sabemos de quién es el registro). Si no, se ofrece un combo buscable.
        JTextField campoIdPacienteFijo = null;
        JComboBox<Paciente> comboPaciente = null;

        if (idPacienteActual != null) {
            campoIdPacienteFijo = new JTextField(18);
            campoIdPacienteFijo.setText(idPacienteActual + " - " + nombrePacienteActual);
            campoIdPacienteFijo.setEditable(false);
            campoIdPacienteFijo.setBackground(new Color(235, 235, 235));
        } else {
            comboPaciente = new JComboBox<>();
            configurarComboBuscable(comboPaciente, controladorPaciente.listarPacientes(),
                    (Paciente p, String texto) ->
                            p.getNombre().toLowerCase().contains(texto)
                                    || p.getIdPaciente().toLowerCase().contains(texto),
                    (Paciente p) -> p.getIdPaciente() + " - " + p.getNombre());
            comboPaciente.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Paciente) {
                        Paciente p = (Paciente) value;
                        setText(p.getIdPaciente() + " - " + p.getNombre());
                    }
                    return c;
                }
            });
        }

        // Variables efectivamente finales, necesarias para usarlas dentro de la lambda del botón Guardar.
        final JTextField campoIdPacienteFinal = campoIdPacienteFijo;
        final JComboBox<Paciente> comboPacienteFinal = comboPaciente;

        JComboBox<TrabajadorHospital> comboAutor = new JComboBox<>();
        configurarComboBuscable(comboAutor, controladorTrabajadores.listarTrabajadores(),
                (TrabajadorHospital t, String texto) ->
                        t.getNombreCompleto().toLowerCase().contains(texto)
                                || t.getIdTrabajador().toLowerCase().contains(texto),
                (TrabajadorHospital t) -> t.getIdTrabajador() + " - " + t.getNombreCompleto());
        comboAutor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TrabajadorHospital) {
                    TrabajadorHospital t = (TrabajadorHospital) value;
                    setText(t.getIdTrabajador() + " - " + t.getNombreCompleto());
                }
                return c;
            }
        });

        // --- Tipo de registro ---
        JComboBox<TipoRegistro> comboTipo = new JComboBox<>(TipoRegistro.values());
        comboTipo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TipoRegistro) {
                    setText(etiquetaTipo((TipoRegistro) value));
                }
                return c;
            }
        });

        // Panel multiopciones
        CardLayout cardLayout = new CardLayout();
        JPanel panelContenidoDinamico = new JPanel(cardLayout);

        JTextArea campoContenido = new JTextArea(5, 22);
        campoContenido.setLineWrap(true);
        campoContenido.setWrapStyleWord(true);
        JPanel panelTexto = new JPanel(new BorderLayout());
        panelTexto.setBorder(BorderFactory.createTitledBorder("Contenido / Descripción"));
        panelTexto.add(new JScrollPane(campoContenido), BorderLayout.CENTER);

        JTextField campoTemperatura = new JTextField(6);
        JTextField campoFrecCardiaca = new JTextField(6);
        JTextField campoPresionSistolica = new JTextField(6);
        JTextField campoPresionDiastolica = new JTextField(6);
        JTextField campoFrecRespiratoria = new JTextField(6);
        JTextField campoSaturacion = new JTextField(6);
        JTextArea campoObservaciones = new JTextArea(2, 22);
        campoObservaciones.setLineWrap(true);
        campoObservaciones.setWrapStyleWord(true);

        JPanel panelSignos = construirPanelSignosVitales(
                campoTemperatura, campoFrecCardiaca, campoPresionSistolica,
                campoPresionDiastolica, campoFrecRespiratoria, campoSaturacion, campoObservaciones);

        panelContenidoDinamico.add(panelTexto, "TEXTO");
        panelContenidoDinamico.add(panelSignos, "SIGNOS");

        comboTipo.addActionListener(e -> {
            TipoRegistro seleccionado = (TipoRegistro) comboTipo.getSelectedItem();
            cardLayout.show(panelContenidoDinamico,
                    seleccionado == TipoRegistro.SIGNOS_VITALES ? "SIGNOS" : "TEXTO");
        });

        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Paciente:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(idPacienteActual != null ? campoIdPacienteFinal : comboPacienteFinal, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Autor (trabajador):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(comboAutor, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("Tipo de Registro:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(comboTipo, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2; gbc.weightx = 1;
        panelForm.add(panelContenidoDinamico, gbc);
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
            String idPacienteSeleccionado = idPacienteActual != null
                    ? idPacienteActual
                    : (comboPacienteFinal.getSelectedItem() instanceof Paciente
                            ? ((Paciente) comboPacienteFinal.getSelectedItem()).getIdPaciente() : null);

            TrabajadorHospital autorSeleccionado = comboAutor.getSelectedItem() instanceof TrabajadorHospital
                    ? (TrabajadorHospital) comboAutor.getSelectedItem() : null;

            String resultado = validarYGuardarRegistro(
                    idPacienteSeleccionado, autorSeleccionado,
                    (TipoRegistro) comboTipo.getSelectedItem(),
                    campoContenido.getText(),
                    campoTemperatura.getText(), campoFrecCardiaca.getText(),
                    campoPresionSistolica.getText(), campoPresionDiastolica.getText(),
                    campoFrecRespiratoria.getText(), campoSaturacion.getText(),
                    campoObservaciones.getText());

            if (resultado == null) {
                dialogo.dispose();
                refrescarTabla();
                JOptionPane.showMessageDialog(this, "Registro clínico agregado correctamente.");
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

    private JPanel construirPanelSignosVitales(JTextField temperatura, JTextField frecCardiaca,
            JTextField presionSistolica, JTextField presionDiastolica,
            JTextField frecRespiratoria, JTextField saturacion, JTextArea observaciones) {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Signos Vitales"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Object[][] filas = {
                {"Temperatura (°C):", temperatura},
                {"Frecuencia Cardíaca (lpm):", frecCardiaca},
                {"Presión Sistólica (mmHg):", presionSistolica},
                {"Presión Diastólica (mmHg):", presionDiastolica},
                {"Frecuencia Respiratoria (rpm):", frecRespiratoria},
                {"Saturación de Oxígeno (%):", saturacion},
        };

        int y = 0;
        for (Object[] fila : filas) {
            gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0;
            panel.add(new JLabel((String) fila[0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            panel.add((JTextField) fila[1], gbc);
            y++;
        }

        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
        panel.add(new JLabel("Observaciones (opcional):"), gbc);
        y++;
        gbc.gridy = y;
        panel.add(new JScrollPane(observaciones), gbc);

        return panel;
    }

    // --- 7. VALIDACIONES ---

    private String validarYGuardarRegistro(String idPaciente, TrabajadorHospital autor, TipoRegistro tipo,
            String contenidoTexto, String temperaturaTexto, String frecCardiacaTexto,
            String presionSistolicaTexto, String presionDiastolicaTexto,
            String frecRespiratoriaTexto, String saturacionTexto, String observacionesTexto) {

        StringBuilder errores = new StringBuilder();

        if (idPaciente == null) {
            errores.append("• Debe seleccionar un paciente de la lista.<br>");
        }

        if (autor == null) {
            errores.append("• Debe seleccionar un autor (trabajador) de la lista.<br>");
        }

        if (tipo == null) {
            errores.append("• Debe seleccionar un tipo de registro.<br>");
        }

        String contenidoFinal = null;

        if (tipo == TipoRegistro.SIGNOS_VITALES) {
            Double temperatura = validarDecimal(temperaturaTexto, 30.0, 45.0);
            if (temperatura == null) {
                errores.append("• Temperatura: debe ser un número entre 30.0 y 45.0 °C.<br>");
            }
            Integer frecCardiaca = validarEntero(frecCardiacaTexto, 20, 250);
            if (frecCardiaca == null) {
                errores.append("• Frecuencia Cardíaca: debe ser entre 20 y 250 lpm.<br>");
            }
            Integer sistolica = validarEntero(presionSistolicaTexto, 50, 250);
            if (sistolica == null) {
                errores.append("• Presión Sistólica: debe ser entre 50 y 250 mmHg.<br>");
            }
            Integer diastolica = validarEntero(presionDiastolicaTexto, 30, 150);
            if (diastolica == null) {
                errores.append("• Presión Diastólica: debe ser entre 30 y 150 mmHg.<br>");
            }
            if (sistolica != null && diastolica != null && diastolica >= sistolica) {
                errores.append("• La presión diastólica debe ser menor que la sistólica.<br>");
            }
            Integer frecRespiratoria = validarEntero(frecRespiratoriaTexto, 5, 60);
            if (frecRespiratoria == null) {
                errores.append("• Frecuencia Respiratoria: debe ser entre 5 y 60 rpm.<br>");
            }
            Integer saturacion = validarEntero(saturacionTexto, 0, 100);
            if (saturacion == null) {
                errores.append("• Saturación de Oxígeno: debe ser entre 0 y 100%.<br>");
            }
            String observaciones = observacionesTexto == null ? "" : observacionesTexto.trim();
            if (!observaciones.isEmpty() && !PATRON_CONTIENE_TEXTO.matcher(observaciones).matches()) {
                errores.append("• Las observaciones no pueden ser solo números.<br>");
            }

            if (errores.length() == 0) {
                contenidoFinal = String.format(
                        "Signos vitales - Temp: %.1f°C | FC: %d lpm | PA: %d/%d mmHg | FR: %d rpm | SpO2: %d%%",
                        temperatura, frecCardiaca, sistolica, diastolica, frecRespiratoria, saturacion);
                if (!observaciones.isEmpty()) {
                    contenidoFinal += " | Obs: " + observaciones;
                }
            }
        } else if (tipo != null) {
            String contenido = contenidoTexto == null ? "" : contenidoTexto.trim();
            if (contenido.isEmpty()) {
                errores.append("• El contenido no puede estar vacío.<br>");
            } else if (contenido.length() < 5) {
                errores.append("• El contenido es demasiado corto (mínimo 5 caracteres).<br>");
            } else if (!PATRON_CONTIENE_TEXTO.matcher(contenido).matches()) {
                errores.append("• El contenido debe incluir texto descriptivo, no solo números.<br>");
            } else {
                contenidoFinal = contenido;
            }
        }

        if (errores.length() > 0) {
            return errores.toString();
        }

        boolean ok = controlador.agregarRegistroPaciente(idPaciente, tipo, contenidoFinal, autor);
        if (!ok) {
            return "• No se pudo agregar el registro: verifique que el paciente exista.";
        }

        return null; // Todo correcto, sin errores
    }

    private Integer validarEntero(String texto, int min, int max) {
        if (texto == null) return null;
        texto = texto.trim();
        if (!PATRON_ENTERO.matcher(texto).matches()) return null;
        int valor = Integer.parseInt(texto);
        if (valor < min || valor > max) return null;
        return valor;
    }

    private Double validarDecimal(String texto, double min, double max) {
        if (texto == null) return null;
        texto = texto.trim();
        if (!PATRON_DECIMAL.matcher(texto).matches()) return null;
        double valor = Double.parseDouble(texto);
        if (valor < min || valor > max) return null;
        return valor;
    }

    // --- 8. COMBOS BUSCABLES (autocompletado que reordena, no oculta) ---

    // Convierte un JComboBox en un campo editable donde, al escribir, las
    // coincidencias (por id o nombre) suben al principio de la lista, sin
    // ocultar el resto. Muestra scroll cuando hay muchos elementos (via
    // setMaximumRowCount), y muestra el texto formateado en vez del
    // toString() crudo del objeto.
    private <T> void configurarComboBuscable(JComboBox<T> combo, List<T> listaCompleta,
                                              BiFunction<T, String, Boolean> coincide,
                                              Function<T, String> formateador) {

        // Flag simple: solo protege al editor mientras reconstruimos el modelo,
        // para que no se sobreescriba lo que el usuario está escribiendo.
        final boolean[] actualizandoModelo = {false};

        combo.setEditable(true);
        combo.setMaximumRowCount(6); // altura fija del desplegable -> aparece scroll con más de 6 elementos

        DefaultComboBoxModel<T> modeloInicial = new DefaultComboBoxModel<>();
        for (T item : listaCompleta) {
            modeloInicial.addElement(item);
        }
        combo.setModel(modeloInicial);

        combo.setEditor(new javax.swing.plaf.basic.BasicComboBoxEditor() {
            @Override
            public void setItem(Object item) {
                if (actualizandoModelo[0]) return; // no tocar el texto mientras reordenamos
                if (item == null) {
                    editor.setText("");
                } else {
                    @SuppressWarnings("unchecked")
                    T valor = (T) item;
                    editor.setText(formateador.apply(valor));
                }
            }
        });

        JTextField campoEditor = (JTextField) combo.getEditor().getEditorComponent();
        combo.setSelectedItem(null); // arranca vacío al abrir el formulario

        campoEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { programarReordenamiento(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { programarReordenamiento(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }

            private void programarReordenamiento() {
                // CLAVE: no tocamos el Document aquí mismo. Se difiere a *después*
                // de que Swing termine de procesar la tecla actual -- eso evita el
                // beep y hace que el filtro se recalcule bien en cada tecla,
                // incluida borrar hasta dejarlo vacío.
                SwingUtilities.invokeLater(this::reordenar);
            }

            private void reordenar() {
                String texto = campoEditor.getText();
                String textoLower = texto.toLowerCase();
                int caret = campoEditor.getCaretPosition();

                List<T> coincidentes = new ArrayList<>();
                List<T> resto = new ArrayList<>();
                for (T item : listaCompleta) {
                    if (texto.isEmpty() || coincide.apply(item, textoLower)) {
                        coincidentes.add(item);
                    } else {
                        resto.add(item);
                    }
                }

                DefaultComboBoxModel<T> modelo = new DefaultComboBoxModel<>();
                for (T item : coincidentes) modelo.addElement(item);
                for (T item : resto) modelo.addElement(item);

                actualizandoModelo[0] = true;
                try {
                    combo.setModel(modelo);
                    combo.setSelectedItem(null); // nunca "seleccionar" automático mientras el usuario escribe
                } finally {
                    actualizandoModelo[0] = false;
                }

                // Refrescar el popup para que se vea el nuevo orden en vivo,
                // sin robarle el foco al campo de texto.
                if (combo.isPopupVisible()) {
                    combo.hidePopup();
                    combo.showPopup();
                } else if (!texto.isEmpty()) {
                    combo.showPopup();
                }

                campoEditor.requestFocusInWindow();
                campoEditor.setCaretPosition(Math.min(caret, campoEditor.getText().length()));
            }
        });
    }
}