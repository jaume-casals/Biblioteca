package presentacio.renderers;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import herramienta.i18n.I18n;
import presentacio.util.UIComponents;

public class RenderitzadorCasellaLlegit extends JCheckBox implements TableCellRenderer {
    public RenderitzadorCasellaLlegit() {
        setHorizontalAlignment(JCheckBox.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
            boolean focus, int row, int col) {
        setSelected(I18n.t("filter_read").equals(val));
        UIComponents.applySelectedColors(this, selected);
        return this;
    }
}