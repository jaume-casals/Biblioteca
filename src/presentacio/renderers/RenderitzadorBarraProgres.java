package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.ui.UITheme;
import presentacio.models.ModelTaulaBiblioteca;

public class RenderitzadorBarraProgres extends JProgressBar implements TableCellRenderer {

    public RenderitzadorBarraProgres() {
        setMinimum(0);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        try {
            int modelRow = t.convertRowIndexToModel(row);
            Llibre l = ((ModelTaulaBiblioteca) t.getModel()).obtenirBookAt(modelRow);
            if (l != null && l.obtenirPagines() > 0) {
                setMaximum(l.obtenirPagines());
                setValue(l.obtenirPaginesLlegides());
                setString(l.obtenirPaginesLlegides() + " / " + l.obtenirPagines());
            } else {
                setMaximum(1); setValue(0); setString("\u2014");
            }
        } catch (Exception ignored) {
            setMaximum(1); setValue(0); setString("\u2014");
        }
        setBackground(selected ? UITheme.palette().accent() : UITheme.palette().bgPanel());
        setForeground(selected ? Color.WHITE : UITheme.palette().textDark());
        return this;
    }
}