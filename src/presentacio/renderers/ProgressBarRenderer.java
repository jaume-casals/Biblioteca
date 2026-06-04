package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.UITheme;
import presentacio.BibliotecaTableModel;

public class ProgressBarRenderer extends JProgressBar implements TableCellRenderer {

    public ProgressBarRenderer() {
        setMinimum(0);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        try {
            int modelRow = t.convertRowIndexToModel(row);
            Llibre l = ((BibliotecaTableModel) t.getModel()).getBookAt(modelRow);
            if (l != null && l.getPagines() > 0) {
                setMaximum(l.getPagines());
                setValue(l.getPaginesLlegides());
                setString(l.getPaginesLlegides() + " / " + l.getPagines());
            } else {
                setMaximum(1); setValue(0); setString("\u2014");
            }
        } catch (Exception ignored) {
            setMaximum(1); setValue(0); setString("\u2014");
        }
        setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
        setForeground(selected ? Color.WHITE : UITheme.TEXT_DARK);
        return this;
    }
}