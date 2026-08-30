package view;

import javax.swing.*;
import java.awt.*;

import controller.ControladorTrabajadores;
import model.TrabajadorHospital;
import model.Doctor;
import model.Enfermero;
import model.NivelExperiencia;

public class guiTrabajadores extends JFrame implements IguiTrabajadores {

    private IGUIPrincipal guiPrincipal;
    private final ControladorTrabajadores controlador;

    public guiTrabajadores(ControladorTrabajadores controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Gestión de Trabajadors");
        setSize(420, 340);
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
        JPanel panel = new JPanel(new GridLayout(6, 1, 12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JButton btnRegistrar = new JButton("Registrar Trabajador");
        JButton btnEditar = new JButton("Editar Trabajador");
        JButton btnEliminar = new JButton("Eliminar Trabajador");
        JButton btnListar = new JButton("Listar Trabajadors");
        JButton btnVolver = new JButton("Volver");

        btnRegistrar.addActionListener(e -> registrarTrabajador());
        btnEditar.addActionListener(e -> editarTrabajador());
        btnEliminar.addActionListener(e -> eliminarTrabajador());
        btnListar.addActionListener(e -> listarTrabajadores());
        btnVolver.addActionListener(e -> volver());

        panel.add(btnRegistrar);
        panel.add(btnEditar);
        panel.add(btnEliminar);
        panel.add(btnListar);
        panel.add(btnVolver);

        setContentPane(panel);
        revalidate();
        repaint();
    }

    @Override
    public void registrarTrabajador() {
        String id = JOptionPane.showInputDialog(this, "ID del trabajador:");
        if (id == null || id.isBlank()) return;

        String nombre = JOptionPane.showInputDialog(this, "Nombre completo:");
        if (nombre == null || nombre.isBlank()) return;

        String[] roles = {"Doctor", "Enfermero"};
        String rol = (String) JOptionPane.showInputDialog(
                this, "Seleccione el rol:", "Rol del trabajador",
                JOptionPane.QUESTION_MESSAGE, null, roles, roles[0]);
        if (rol == null) return;

        TrabajadorHospital rolEspecifico = capturarDatosDeRol(id, nombre, rol);
        if (rolEspecifico == null) return; // el trabajador canceló la captura de datos del rol

        boolean ok = controlador.registrarTrabajador(id, nombre, rolEspecifico);
        JOptionPane.showMessageDialog(this, ok
                ? "Trabajador registrado correctamente."
                : "No se pudo registrar: datos inválidos o el ID ya existe.");
    }

    /** Pide los datos propios del rol elegido y construye la subclase de TrabajadorHospital correspondiente. */
    private TrabajadorHospital capturarDatosDeRol(String id, String nombre, String rol) {
        if ("Doctor".equals(rol)) {
            String especialidad = JOptionPane.showInputDialog(this, "Especialidad:");
            if (especialidad == null || especialidad.isBlank()) return null;
            return new Doctor(id, nombre, especialidad);
        } else {
            NivelExperiencia nivel = (NivelExperiencia) JOptionPane.showInputDialog(
                    this, "Nivel de experiencia:", "Enfermero",
                    JOptionPane.QUESTION_MESSAGE, null,
                    NivelExperiencia.values(), NivelExperiencia.values()[0]);
            if (nivel == null) return null;
            return new Enfermero(id, nombre, nivel);
        }
    }

    @Override
    public void editarTrabajador() {
        String id = JOptionPane.showInputDialog(this, "ID del trabajador a editar:");
        if (id == null || id.isBlank()) return;

        TrabajadorHospital existente = controlador.buscarTrabajadorPorId(id);
        if (existente == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el trabajador.");
            return;
        }

        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", existente.getNombreCompleto());
        if (nuevoNombre == null || nuevoNombre.isBlank()) return;

        TrabajadorHospital actualizado;
        if (existente instanceof Doctor) {
            Doctor doctor = (Doctor) existente;
            String especialidad = JOptionPane.showInputDialog(this, "Nueva especialidad:", doctor.getEspecialidad());
            if (especialidad == null || especialidad.isBlank()) return;
            actualizado = new Doctor(id, nuevoNombre, especialidad);
        } else if (existente instanceof Enfermero) {
            Enfermero enfermero = (Enfermero) existente;
            NivelExperiencia nivel = (NivelExperiencia) JOptionPane.showInputDialog(
                    this, "Nuevo nivel de experiencia:", "Enfermero",
                    JOptionPane.QUESTION_MESSAGE, null,
                    NivelExperiencia.values(), enfermero.getNivelExperiencia());
            if (nivel == null) return;
            actualizado = new Enfermero(id, nuevoNombre, nivel);
        } else {
            JOptionPane.showMessageDialog(this, "Tipo de trabajador no soportado.");
            return;
        }

        boolean ok = controlador.editarTrabajador(id, nuevoNombre, actualizado);
        JOptionPane.showMessageDialog(this, ok ? "Trabajador actualizado." : "No se pudo actualizar el trabajador.");
    }

    @Override
    public void eliminarTrabajador() {
        String id = JOptionPane.showInputDialog(this, "ID del trabajador a eliminar:");
        if (id == null) return;
        boolean ok = controlador.eliminarTrabajador(id);
        JOptionPane.showMessageDialog(this, ok ? "Trabajador eliminado." : "No se encontró el trabajador.");
    }

    @Override
    public void listarTrabajadores() {
        StringBuilder sb = new StringBuilder();
        controlador.listarTrabajadores().forEach(u -> sb.append(u).append("\n"));
        JOptionPane.showMessageDialog(this, sb.length() == 0 ? "No hay trabajadors registrados." : sb.toString());
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}