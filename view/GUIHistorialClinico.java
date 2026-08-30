package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import controller.ControladorHistorialClinico;
import controller.ControladorTrabajadores;
import model.TipoRegistro;
import model.TrabajadorHospital;
import model.RegistroConPaciente;
import model.RegistroClinico;

public class GUIHistorialClinico extends JFrame implements IGUIHistorialClinico {

    private IGUIPrincipal guiPrincipal;
    private final ControladorHistorialClinico controlador;
    private final ControladorTrabajadores controladorTrabajadores;

    private JLabel lblEstado;
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private List<RegistroConPaciente> registrosActuales;

    private static final String[] COLUMNAS = {
            "ID Paciente", "Paciente", "Fecha y Hora", "Tipo", "Autor", "Contenido"
    };
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    //validaciones
    private static final Pattern PATRON_ID =
            Pattern.compile("^[A-Za-z0-9\\-]{1,20}$");
    private static final Pattern PATRON_CONTIENE_TEXTO =
            Pattern.compile(".*[A-Za-zÁÉÍÓÚÑÜáéíóúñü].*");
    private static final Pattern PATRON_ENTERO =
            Pattern.compile("^\\d{1,4}$");
    private static final Pattern PATRON_DECIMAL =
            Pattern.compile("^\\d{1,3}(\\.\\d{1,2})?$");

    public GUIHistorialClinico(ControladorHistorialClinico controlador, ControladorTrabajadores controladorTrabajadores) {
        this.controlador = controlador;
        this.controladorTrabajadores = controladorTrabajadores;
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

        JLabel titulo = new JLabel("Registros Clínicos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

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

    //apariencia tipo excel :)
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

        //mas cosas para que se vea mas bonita la lista
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

    private void refrescarTabla() {
        registrosActuales = controlador.obtenerTodosLosRegistros();
        modeloTabla.setRowCount(0);

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

    private static String etiquetaTipo(TipoRegistro tipo) {
        switch (tipo) {
            case DIAGNOSTICO: return "Diagnóstico";
            case EVOLUCION: return "Evolución";
            case MEDICACION: return "Medicación";
            case SIGNOS_VITALES: return "Signos Vitales";
            default: return tipo.toString();
        }
    }

    //formulario

    @Override
    public void agregarRegistroClinico() {
        JDialog dialogo = new JDialog(this, "Agregar Registro Clínico", true);
        dialogo.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(15, 15, 5, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField campoIdPaciente = new JTextField(18);
        JTextField campoIdAutor = new JTextField(18);
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

        //panel multiopciones
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
        panelForm.add(new JLabel("ID del Paciente:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoIdPaciente, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panelForm.add(new JLabel("ID del Autor (trabajador):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelForm.add(campoIdAutor, gbc);

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
            String resultado = validarYGuardarRegistro(
                    campoIdPaciente.getText(), campoIdAutor.getText(),
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

    //Seccion para validar TODO lo que se ingrese en el formulario, se muestra una advertencia en el GUI si hay algo incorrecto
    private String validarYGuardarRegistro(String idPacienteTexto, String idAutorTexto, TipoRegistro tipo,
            String contenidoTexto, String temperaturaTexto, String frecCardiacaTexto,
            String presionSistolicaTexto, String presionDiastolicaTexto,
            String frecRespiratoriaTexto, String saturacionTexto, String observacionesTexto) {

        StringBuilder errores = new StringBuilder();

        String idPaciente = idPacienteTexto == null ? "" : idPacienteTexto.trim();
        String idAutor = idAutorTexto == null ? "" : idAutorTexto.trim();

        if (idPaciente.isEmpty()) {
            errores.append("• El ID del paciente es obligatorio.<br>");
        } else if (!PATRON_ID.matcher(idPaciente).matches()) {
            errores.append("• El ID del paciente solo puede tener letras, números y guiones (sin espacios).<br>");
        }

        if (idAutor.isEmpty()) {
            errores.append("• El ID del autor es obligatorio.<br>");
        } else if (!PATRON_ID.matcher(idAutor).matches()) {
            errores.append("• El ID del autor solo puede tener letras, números y guiones (sin espacios).<br>");
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
                errores.append("• Frecuencia Cardíaca: debe ser un número entero entre 20 y 250 lpm.<br>");
            }
            Integer sistolica = validarEntero(presionSistolicaTexto, 50, 250);
            if (sistolica == null) {
                errores.append("• Presión Sistólica: debe ser un número entero entre 50 y 250 mmHg.<br>");
            }
            Integer diastolica = validarEntero(presionDiastolicaTexto, 30, 150);
            if (diastolica == null) {
                errores.append("• Presión Diastólica: debe ser un número entero entre 30 y 150 mmHg.<br>");
            }
            if (sistolica != null && diastolica != null && diastolica >= sistolica) {
                errores.append("• La presión diastólica debe ser menor que la sistólica.<br>");
            }
            Integer frecRespiratoria = validarEntero(frecRespiratoriaTexto, 5, 60);
            if (frecRespiratoria == null) {
                errores.append("• Frecuencia Respiratoria: debe ser un número entero entre 5 y 60 rpm.<br>");
            }
            Integer saturacion = validarEntero(saturacionTexto, 0, 100);
            if (saturacion == null) {
                errores.append("• Saturación de Oxígeno: debe ser un número entero entre 0 y 100%.<br>");
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

        TrabajadorHospital autor = controladorTrabajadores.buscarTrabajadorPorId(idAutor);
        if (autor == null) {
            return "• No se encontró un trabajador con ese ID.";
        }

        boolean ok = controlador.agregarRegistroPaciente(idPaciente, tipo, contenidoFinal, autor);
        if (!ok) {
            return "• No se pudo agregar el registro: verifique que el ID del paciente exista.";
        }

        return null; //<- No se muestra ningun error si el usuario lleno todo de forma correcta
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

    //Doble click sobre una fila para ver mas detalles

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

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}