package view;

import javax.swing.*;
import java.awt.*;

import controller.ControladorPaciente;

public class GUIPacientes extends JFrame implements IGUIPacientes {

    private IGUIPrincipal guiPrincipal;
    private final ControladorPaciente controlador;

    public GUIPacientes(ControladorPaciente controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Gestión de Pacientes");
        setSize(420, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    @Override
    public void mostrar() {
        setVisible(true);
    }

    @Override
    public void mostrarOpciones() {
        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 60, 25, 60));

        JButton btnRegistrar = new JButton("Registrar Paciente");
        JButton btnEditar = new JButton("Editar Paciente");
        JButton btnEliminar = new JButton("Eliminar Paciente");
        JButton btnListar = new JButton("Listar Pacientes");
        JButton btnBuscar = new JButton("Buscar Paciente");
        JButton btnVolver = new JButton("Volver");

        btnRegistrar.addActionListener(e -> registrarPaciente());
        btnEditar.addActionListener(e -> editarPaciente());
        btnEliminar.addActionListener(e -> eliminarPaciente());
        btnListar.addActionListener(e -> listarPacientes());
        btnBuscar.addActionListener(e -> buscarPaciente());
        btnVolver.addActionListener(e -> volver());

        panel.add(btnRegistrar);
        panel.add(btnEditar);
        panel.add(btnEliminar);
        panel.add(btnListar);
        panel.add(btnBuscar);
        panel.add(btnVolver);

        setContentPane(panel);
        revalidate();
        repaint();
    }

    @Override
    public void registrarPaciente() {
        String id = JOptionPane.showInputDialog(this, "ID del paciente:");
        if (id == null || id.isBlank()) return;

        String nombre = JOptionPane.showInputDialog(this, "Nombre completo:");
        if (nombre == null || nombre.isBlank()) return;

        String edadTexto = JOptionPane.showInputDialog(this, "Edad:");
        if (edadTexto == null) return;
        int edad;
        try {
            edad = Integer.parseInt(edadTexto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Edad inválida.");
            return;
        }

        String habitacionTexto = JOptionPane.showInputDialog(this, "Número de habitación:");
        if (habitacionTexto == null) return;
        int habitacion;
        try {
            habitacion = Integer.parseInt(habitacionTexto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Número de habitación inválido.");
            return;
        }

        boolean ok = controlador.registrarPaciente(id, nombre, edad, habitacion);
        JOptionPane.showMessageDialog(this, ok
                ? "Paciente registrado correctamente."
                : "No se pudo registrar el paciente (verifique que el ID no exista).");
    }

    @Override
    public void editarPaciente() {
        String id = JOptionPane.showInputDialog(this, "ID del paciente a editar:");
        if (id == null || id.isBlank()) return;

        String habitacionTexto = JOptionPane.showInputDialog(this, "Nueva habitación:");
        if (habitacionTexto == null) return;
        int nuevaHabitacion;
        try {
            nuevaHabitacion = Integer.parseInt(habitacionTexto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Número de habitación inválido.");
            return;
        }

        boolean ok = controlador.editarPaciente(id, nuevaHabitacion);
        JOptionPane.showMessageDialog(this, ok
                ? "Paciente actualizado correctamente."
                : "No se encontró el paciente.");
    }

    @Override
    public void eliminarPaciente() {
        String id = JOptionPane.showInputDialog(this, "ID del paciente a eliminar:");
        if (id == null) return;
        boolean ok = controlador.eliminarPaciente(id);
        JOptionPane.showMessageDialog(this, ok ? "Paciente eliminado." : "No se encontró el paciente.");
    }

    @Override
    public void listarPacientes() {
        StringBuilder sb = new StringBuilder();
        controlador.listarPacientes().forEach(p -> sb.append(p).append("\n"));
        JOptionPane.showMessageDialog(this, sb.length() == 0 ? "No hay pacientes registrados." : sb.toString());
    }

    @Override
    public void buscarPaciente() {
        String id = JOptionPane.showInputDialog(this, "ID del paciente a buscar:");
        if (id == null) return;
        boolean existe = controlador.buscarPorId(id);
        JOptionPane.showMessageDialog(this, existe ? "El paciente existe." : "No se encontró el paciente.");
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}