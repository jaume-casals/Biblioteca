package presentacio.config;

import herramienta.config.Configuracio;
import herramienta.config.ConfiguracioDb;
import persistencia.contract.EscritorBiblioteca;
import presentacio.util.UIComponents;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import static herramienta.i18n.I18n.t;

/** Secció de dades: visualització de mida de BBDD, botó de buidar biblioteca, selector de perfil. */
public final class ConfiguracioDataSection {
    private ConfiguracioDataSection() {}

    public static String sanitizeProfileName(String name) {
        return name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public static JPanel build(JDialog owner, EscritorBiblioteca cd, ConfiguracioDialogListener listener) {
        UIComponents.SectionPanel section = UIComponents.sectionPanel();
        JPanel panel = section.panel();
        GroupLayout gl = section.layout();

        JLabel lblSeccio = UIComponents.sectionHeader("lbl_data");

        JLabel lblDbSize = UIComponents.label("lbl_db_size");
        JLabel lblDbSizeVal = new JLabel("...");
        lblDbSizeVal.setFont(herramienta.ui.UITheme.fontBase());
        lblDbSizeVal.setForeground(herramienta.ui.UITheme.palette().textDark());
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() { return cd.obtenirDbSizeBytes(); }
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
        UIComponents.styleDangerButton(btnBuidar);
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
                cd.netejarAll();
                if (listener != null) listener.enRefrescarDades();
                JOptionPane.showMessageDialog(owner, t("dlg_clear_done"),
                    t("dlg_clear_done_title"), JOptionPane.INFORMATION_MESSAGE);
                owner.dispose();
            } catch (Exception ex) {
                new herramienta.ui.DialegError(ex).mostrarErrorMessage();
            }
        });

        JButton btnPerfils = new JButton(t("btn_gestio_perfils"));
        UIComponents.styleSecondaryButton(btnPerfils);
        btnPerfils.setToolTipText(t("tip_gestio_perfils"));
        btnPerfils.addActionListener(e -> {
            if (!"h2".equals(Configuracio.obtenirDbType())) {
                JOptionPane.showMessageDialog(owner,
                    t("dlg_perfils_h2_only"),
                    t("dlg_perfils_bd_title"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            java.util.List<String> profiles = Configuracio.listDbProfiles();
            String[] opts = profiles.toArray(new String[0]);
            String current = Configuracio.obtenirDbProfile();
            String chosen = (String) JOptionPane.showInputDialog(owner,
                t("dlg_perfil_actiu", current),
                t("dlg_canviar_perfil_title"), JOptionPane.PLAIN_MESSAGE, null, opts, current);
            if (chosen == null) return;
            String sanitized = sanitizeProfileName(chosen);
            if (sanitized.isEmpty() || sanitized.equals(current)) return;
            if (!sanitized.equals(chosen)) {
                int warn = JOptionPane.showConfirmDialog(owner,
                    t("dlg_perfil_nom_invalid", sanitized), t("dlg_perfil_nom_invalid_title"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (warn != JOptionPane.YES_OPTION) return;
            }
            chosen = sanitized;
            int confirm = JOptionPane.showConfirmDialog(owner,
                t("dlg_perfil_confirmar", chosen), t("dlg_perfil_confirmar_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            ConfiguracioDb.posarProfile(chosen);
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
        // La secció de dades no té camps editables per l'usuari per refrescar;
        // la mida de la BBDD es recalcula a cada obertura amb el SwingWorker
        // de build().
    }
}
