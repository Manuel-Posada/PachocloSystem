package view;

import javax.swing.*;
import java.awt.*;

import controller.ControladorUsuarios;

/**
 * Esqueleto navegable de la pantalla de Usuarios.
 * TODO: reemplazar el contenido de mostrarOpciones() por los
 * formularios reales (registrar, editar, eliminar, listar),
 * apoyándose en ControladorUsuarios.
 */
public class GUIUsuarios extends JFrame implements IGUIUsuarios {

    private IGUIPrincipal guiPrincipal;
    private final ControladorUsuarios controlador;

    public GUIUsuarios(ControladorUsuarios controlador) {
        this.controlador = controlador;
        configurarVentana();
        mostrarOpciones();
    }

    /** Setter injection: se asigna después de construir GUIPrincipal, evitando dependencia circular en el constructor. */
    public void setGuiPrincipal(IGUIPrincipal guiPrincipal) {
        this.guiPrincipal = guiPrincipal;
    }

    private void configurarVentana() {
        setTitle("Gestión de Usuarios");
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

        JButton btnRegistrar = new JButton("Registrar Usuario");
        JButton btnEditar = new JButton("Editar Usuario");
        JButton btnEliminar = new JButton("Eliminar Usuario");
        JButton btnListar = new JButton("Listar Usuarios");
        JButton btnVolver = new JButton("Volver");

        btnRegistrar.addActionListener(e -> registrarUsuario());
        btnEditar.addActionListener(e -> editarUsuario());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnListar.addActionListener(e -> listarUsuarios());
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
    public void registrarUsuario() {
        // TODO: reemplazar por un formulario real; esto es solo para validar el flujo end-to-end
        String id = JOptionPane.showInputDialog(this, "ID del usuario:");
        if (id == null) return;
        String nombre = JOptionPane.showInputDialog(this, "Nombre:");
        if (nombre == null) return;
        JOptionPane.showMessageDialog(this, "TODO: falta capturar el rolEspecifico (TrabajadorHospital)");
        // boolean ok = controlador.registrarUsuario(id, nombre, rolEspecifico);
    }

    @Override
    public void editarUsuario() {
        JOptionPane.showMessageDialog(this, "TODO: formulario de edición de usuario");
    }

    @Override
    public void eliminarUsuario() {
        String id = JOptionPane.showInputDialog(this, "ID del usuario a eliminar:");
        if (id == null) return;
        boolean ok = controlador.eliminarUsuario(id);
        JOptionPane.showMessageDialog(this, ok ? "Usuario eliminado." : "No se encontró el usuario.");
    }

    @Override
    public void listarUsuarios() {
        StringBuilder sb = new StringBuilder();
        controlador.listarUsuarios().forEach(u -> sb.append(u).append("\n"));
        JOptionPane.showMessageDialog(this, sb.length() == 0 ? "No hay usuarios registrados." : sb.toString());
    }

    @Override
    public void volver() {
        setVisible(false);
        guiPrincipal.mostrar();
    }
}