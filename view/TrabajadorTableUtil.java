package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.Doctor;
import model.Enfermero;
import model.NivelExperiencia;
import model.TrabajadorHospital;

//Solo contiene logica de tabla (estilos, columnas, e.t.c), NO HAY regla de negocio ni validaciones
final class TrabajadorTableUtil {

    private static final String[] COLUMNAS_SIN_ACCIONES = {"ID", "Nombre Completo", "Rol", "Detalle"};
    private static final String[] COLUMNAS_CON_ACCIONES = {"ID", "Nombre Completo", "Rol", "Detalle", "Acciones"};

    private TrabajadorTableUtil() {
    }

    static DefaultTableModel crearModelo(boolean conColumnaAcciones) {
        String[] columnas = conColumnaAcciones ? COLUMNAS_CON_ACCIONES : COLUMNAS_SIN_ACCIONES;
        int columnaAcciones = columnas.length - 1;

        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return conColumnaAcciones && column == columnaAcciones;
            }
        };
    }

    static void estilizarTabla(JTable tabla, boolean conColumnaAcciones) {
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(210, 210, 210));
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setRowHeight(conColumnaAcciones ? 34 : 26);
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
        int columnasDeTexto = conColumnaAcciones ? tabla.getColumnCount() - 1 : tabla.getColumnCount();
        for (int i = 0; i < columnasDeTexto; i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        tabla.getColumnModel().getColumn(0).setPreferredWidth(90);   // ID
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);  // Nombre
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);  // Rol
        tabla.getColumnModel().getColumn(3).setPreferredWidth(conColumnaAcciones ? 220 : 300); // Detalle
        if (conColumnaAcciones) {
            tabla.getColumnModel().getColumn(4).setPreferredWidth(140); // Acciones
        }
    }

    static void poblarFilas(DefaultTableModel modelo, List<TrabajadorHospital> trabajadores, boolean conColumnaAcciones) {
        modelo.setRowCount(0);
        for (TrabajadorHospital t : trabajadores) {
            if (conColumnaAcciones) {
                modelo.addRow(new Object[]{
                        t.getIdTrabajador(), t.getNombreCompleto(), etiquetaRol(t), detalleRol(t), ""
                });
            } else {
                modelo.addRow(new Object[]{
                        t.getIdTrabajador(), t.getNombreCompleto(), etiquetaRol(t), detalleRol(t)
                });
            }
        }
    }

    static String etiquetaRol(TrabajadorHospital t) {
        if (t instanceof Doctor) return "Doctor";
        if (t instanceof Enfermero) return "Enfermero";
        return "Otro";
    }

    static String detalleRol(TrabajadorHospital t) {
        if (t instanceof Doctor) {
            return "Especialidad: " + ((Doctor) t).getEspecialidad();
        }
        if (t instanceof Enfermero) {
            return "Nivel: " + etiquetaNivel(((Enfermero) t).getNivelExperiencia());
        }
        return "";
    }

    static String etiquetaNivel(NivelExperiencia nivel) {
        if (nivel == null) return "";
        switch (nivel) {
            case NOVATO: return "Novato";
            case PRINCIPIANTE: return "Principiante";
            case AVANZADO: return "Avanzado";
            default: return nivel.toString();
        }
    }
}