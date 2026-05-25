package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import herramienta.I18n;
import herramienta.UITheme;

public class BotonDetallesRenderer extends JButton implements TableCellRenderer {
    public BotonDetallesRenderer() { UITheme.styleAccentButton(this); }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        setBackground(isSelected ? UITheme.ACCENT_ALT : UITheme.ACCENT);
        setForeground(Color.WHITE);
        setText(I18n.t("col_details"));
        return this;
    }
}