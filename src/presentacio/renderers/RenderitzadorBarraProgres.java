package presentacio.renderers;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import presentacio.models.ModelTaulaBiblioteca;
import presentacio.util.UIComponents;

public class RenderitzadorBarraProgres extends JProgressBar implements TableCellRenderer {

    public RenderitzadorBarraProgres() {
        setMinimum(0);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value,
            boolean selected, boolean focus, int row, int col) {
        if (t.getModel() instanceof ModelTaulaBiblioteca model) {
            try {
                int modelRow = t.convertRowIndexToModel(row);
                Llibre l = model.obtenirBookAt(modelRow);
                if (l != null && l.obtenirPagines() > 0) {
                    setMaximum(l.obtenirPagines());
                    setValue(l.obtenirPaginesLlegides());
                    setString(l.obtenirPaginesLlegides() + " / " + l.obtenirPagines());
                } else {
                    setMaximum(1); setValue(0); setString("\u2014");
                }
            } catch (RuntimeException ignored) {
                setMaximum(1); setValue(0); setString("\u2014");
            }
        } else {
            setMaximum(1); setValue(0); setString("\u2014");
        }
        UIComponents.applySelectedColors(this, selected);
        return this;
    }
}