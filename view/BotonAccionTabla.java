package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.Consumer;

final class BotonAccionTabla {

    private BotonAccionTabla() {
    }

    static TableCellRenderer crearRenderer(String texto, Color color) {
        return new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
                panel.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
                panel.add(construirBoton(texto, color));
                return panel;
            }
        };
    }

    //al HacerClic recibe el ID (columna 0) de la fila donde se hizo clic
    static TableCellEditor crearEditor(DefaultTableModel modelo, String texto, Color color, Consumer<String> alHacerClic) {
        return new EditorBoton(modelo, texto, color, alHacerClic);
    }

    private static JButton construirBoton(String texto, Color color) {
        JButton boton = new JButton(texto);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD, 11f));
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setMargin(new Insets(2, 10, 2, 10));
        return boton;
    }

    private static class EditorBoton extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel;
        private final DefaultTableModel modelo;
        private int filaActual;

        EditorBoton(DefaultTableModel modelo, String texto, Color color, Consumer<String> alHacerClic) {
            this.modelo = modelo;
            JButton boton = construirBoton(texto, color);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
            panel.add(boton);

            boton.addActionListener(e -> {
                String id = String.valueOf(this.modelo.getValueAt(filaActual, 0));
                fireEditingStopped();
                alHacerClic.accept(id);
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
}