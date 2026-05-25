package presentacio.renderers;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;

import herramienta.I18n;
import herramienta.UITheme;

public class BotonDetallesEditor extends DefaultCellEditor {
    private final JButton botonDetalles;

    public BotonDetallesEditor(JCheckBox checkbox, JButton botonDetalles) {
        super(checkbox);
        this.botonDetalles = botonDetalles;
        UITheme.styleAccentButton(botonDetalles);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int col) {
        botonDetalles.setText(I18n.t("col_details"));
        return botonDetalles;
    }
}