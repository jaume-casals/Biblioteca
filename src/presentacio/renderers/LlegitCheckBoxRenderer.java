package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import herramienta.I18n;
import herramienta.UITheme;

public class LlegitCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
    public LlegitCheckBoxRenderer() {
        setHorizontalAlignment(JCheckBox.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object val, boolean selected,
            boolean focus, int row, int col) {
        setSelected(I18n.t("filter_read").equals(val));
        setBackground(selected ? UITheme.ACCENT : UITheme.BG_PANEL);
        setForeground(selected ? Color.WHITE : UITheme.TEXT_DARK);
        return this;
    }
}