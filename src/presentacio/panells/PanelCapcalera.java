package presentacio.panells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import herramienta.ui.Tokens;
import herramienta.ui.UITheme;
import presentacio.util.UIComponents;

/**
 * Capçalera del contingut.
 *
 * <p>Estructura en tres zones:
 * <ul>
 *   <li><b>Títol de la vista</b> (a l'esquerra): "Tots els llibres", el nom
 *       de la prestatgeria o el nom de la vista ràpida.</li>
 *   <li><b>Recompte</b>: quants llibres corresponen a la vista. El text és
 *       exactament el mateix que mostra la barra d'estat, de manera que no
 *       hi hagi dos comptadors que puguin contradir-se.</li>
 *   <li><b>Acció principal</b> (a la dreta): "Afegir llibre".</li>
 * </ul>
 *
 * <p>La capçalera no rep el focus: és un context, no un control. Tots els
 * seus elements són nonetheless accessibles per lectors de pantalla.
 */
public class PanelCapcalera extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel lblTitol = new JLabel();
    private final JLabel lblComptador = new JLabel();
    private final JButton btnAfegirLlibre = new JButton();

    public PanelCapcalera() {
        setLayout(new BorderLayout(Tokens.SPACE_4, 0));
        setBackground(UITheme.palette().bgPanel());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(Tokens.SPACE_4, Tokens.SPACE_5, Tokens.SPACE_4, Tokens.SPACE_5)));

        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setBackground(UITheme.palette().bgPanel());
        textos.setOpaque(false);

        lblTitol.setFont(UITheme.fontTitle());
        lblTitol.setForeground(UITheme.palette().textDark());
        lblTitol.setText(" ");

        lblComptador.setFont(UITheme.fontBase().deriveFont((float) Tokens.FONT_SIZE_COUNT));
        lblComptador.setForeground(UITheme.palette().textMid());
        lblComptador.setText(" ");

        textos.add(lblTitol, BorderLayout.NORTH);
        textos.add(lblComptador, BorderLayout.SOUTH);
        add(textos, BorderLayout.CENTER);

        JPanel accions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tokens.SPACE_2, 0));
        accions.setBackground(UITheme.palette().bgPanel());
        accions.setOpaque(false);

        btnAfegirLlibre.setText(herramienta.i18n.I18n.t("btn_new_book"));
        UIComponents.styleAccentButton(btnAfegirLlibre);
        btnAfegirLlibre.setPreferredSize(new Dimension(
            Math.max(140, btnAfegirLlibre.getPreferredSize().width), Tokens.CONTROL_HEIGHT));
        accions.add(btnAfegirLlibre);
        add(accions, BorderLayout.EAST);
    }

    public void posarTitol(String titol) {
        lblTitol.setText(titol == null || titol.isBlank() ? " " : titol);
    }

    public void posarComptador(String text) {
        lblComptador.setText(text == null || text.isBlank() ? " " : text);
    }

    public String obtenirTitol() { return lblTitol.getText(); }
    public String obtenirComptador() { return lblComptador.getText(); }

    public JButton obtenirBtnAfegirLlibre() { return btnAfegirLlibre; }

    public void aplicarTheme() {
        setBackground(UITheme.palette().bgPanel());
        // La vora inferior du el color del tema: sense refrescar-la, en mode
        // fosc la línia es quedava amb el color del tema clar.
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(Tokens.SPACE_4, Tokens.SPACE_5, Tokens.SPACE_4, Tokens.SPACE_5)));
        lblTitol.setForeground(UITheme.palette().textDark());
        lblComptador.setForeground(UITheme.palette().textMid());
        UIComponents.styleAccentButton(btnAfegirLlibre);
        repaint();
    }
}
