package presentacio.config;

import herramienta.Config;
import herramienta.UITheme;
import interficie.BibliotecaWriter;
import presentacio.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import static herramienta.I18n.t;

/** Data section: DB size display, clear-library button, profile picker. */
public final class ConfiguracioDataSection {
    private ConfiguracioDataSection() {}

    public static JPanel build(JDialog owner, BibliotecaWriter cd, ConfiguracioDialogListener listener) {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.BG_PANEL);
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);

        JLabel lblSeccio = new JLabel(t("lbl_data"));
        lblSeccio.setFont(UITheme.fontBold());
        lblSeccio.setForeground(UITheme.ACCENT);

        JLabel lblDbSize = new JLabel(t("lbl_db_size"));
        UIComponents.styleLabel(lblDbSize);
        JLabel lblDbSizeVal = new JLabel("...");
        lblDbSizeVal.setFont(UITheme.fontBase());
        lblDbSizeVal.setForeground(UITheme.TEXT_DARK);
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() { return cd.getDbSizeBytes(); }
            @Override protected void done() {
                try {
                    long dbBytes = get();
                    String dbSizeStr = dbBytes < 0 ? "N/D" :
                        dbBytes < 1024 * 1024 ? String.format(java.util.Locale.ROOT, "%.1f KB", dbBytes / 1024.0) :
                        String.format(java.util.Locale.ROOT, "%.2f MB", dbBytes / (1024.0 * 1024.0));
                    lblDbSizeVal.setText(dbSizeStr);
                } catch (Exception ignored) {
                    lblDbSizeVal.setText("N/D");
                }
            }
        }.execute();

        JButton btnBuidar = new JButton(t("btn_clear_library"));
        btnBuidar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnBuidar.setBackground(UITheme.DANGER);
        btnBuidar.setForeground(java.awt.Color.WHITE);
        btnBuidar.setFont(UITheme.fontBold());
        btnBuidar.setFocusPainted(false);
        btnBuidar.setBorderPainted(false);
        btnBuidar.setOpaque(true);
        btnBuidar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btnBuidar.addActionListener(e -> {
            int r1 = JOptionPane.showConfirmDialog(owner,
                t("dlg_confirm_clear_1"),
                t("dlg_confirm_clear_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r1 != JOptionPane.YES_OPTION) return;
            int r2 = JOptionPane.showConfirmDialog(owner,
                t("dlg_confirm_clear_2"),
                t("dlg_confirm_clear_2_title"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (r2 != JOptionPane.YES_OPTION) return;
            try {
                cd.clearAll();
                if (listener != null) listener.onRefreshData();
                JOptionPane.showMessageDialog(owner, t("dlg_clear_done"),
                    t("dlg_clear_done_title"), JOptionPane.INFORMATION_MESSAGE);
                owner.dispose();
            } catch (Exception ex) {
                new herramienta.DialogoError(ex).showErrorMessage();
            }
        });

        JButton btnPerfils = new JButton(t("btn_gestio_perfils"));
        UIComponents.styleSecondaryButton(btnPerfils);
        btnPerfils.setToolTipText(t("tip_gestio_perfils"));
        btnPerfils.addActionListener(e -> {
            if (!"h2".equals(Config.getDbType())) {
                JOptionPane.showMessageDialog(owner,
                    t("dlg_perfils_h2_only"),
                    t("dlg_perfils_bd_title"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            java.util.List<String> profiles = Config.listDbProfiles();
            String[] opts = profiles.toArray(new String[0]);
            String current = Config.getDbProfile();
            String chosen = (String) JOptionPane.showInputDialog(owner,
                t("dlg_perfil_actiu", current),
                t("dlg_canviar_perfil_title"), JOptionPane.PLAIN_MESSAGE, null, opts, current);
            if (chosen == null) return;
            chosen = chosen.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
            if (chosen.isEmpty() || chosen.equals(current)) return;
            int confirm = JOptionPane.showConfirmDialog(owner,
                t("dlg_perfil_confirmar", chosen), t("dlg_perfil_confirmar_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            Config.setDbProfile(chosen);
            String finalChosen = chosen;
            JOptionPane.showMessageDialog(owner,
                t("dlg_perfil_canviat", finalChosen),
                t("dlg_perfil_bd_info_title"), JOptionPane.INFORMATION_MESSAGE);
        });

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lblSeccio)
            .addGroup(gl.createSequentialGroup()
                .addComponent(lblDbSize)
                .addComponent(lblDbSizeVal))
            .addComponent(btnBuidar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(btnPerfils, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addComponent(lblSeccio)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblDbSize).addComponent(lblDbSizeVal))
            .addComponent(btnBuidar)
            .addComponent(btnPerfils)
        );

        return panel;
    }

    public static void reloadFromConfig(JPanel root) {
        // Data section has no user-editable fields to refresh; DB size is
        // recomputed on each open via the SwingWorker in build().
    }
}
