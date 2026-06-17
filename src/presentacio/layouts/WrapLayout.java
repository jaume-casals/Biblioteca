package presentacio.layouts;

import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/*
 * WrapLayout — subclasse de FlowLayout que permet que els components
 * s'ajustin a múltiples files segons l'amplada del viewport pare.
 * Originalment extreta de la classe interna privada de
 * {@code GaleriaCobertesPanel} per a reutilització.
 *
 * Adaptat de l'exemple públic "WrapLayout" del tutorial de Java de Sun:
 *   https://docs.oracle.com/javase/tutorial/uiswing/layout/custom.html
 *
 * Sun Microsystems, Inc. ("Sun") SOFTWARE LICENSE AGREEMENT for the
 * Java(TM) Tutorial Sample Code. El codi original del tutorial és de
 * domini públic per a ús docent; aquesta adaptació conserva la caiguda
 * walk-the-tree de layoutSize() però utilitza una sola caiguda a
 * MAX_VALUE en lloc del patró de doble passada (veure el finding LOW
 * de tot.txt sobre la segona passada inútil).
 */
/**
 * Subclasse de FlowLayout que permet que els components s'ajustin a múltiples files
 * segons l'amplada del viewport pare. Extreta de la classe interna privada de
 * {@code GaleriaCobertesPanel} per a reutilització.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true);  }
    @Override public Dimension minimumLayoutSize(Container t)   { return layoutSize(t, false); }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // Llegeix l'amplada del pare directament; cau a MAX_VALUE al
            // PRIMER intent (segons el finding LOW de tot.txt — el codi
            // antic feia una segona passada per l'arbre per recuperar-se
            // d'un pare d'amplada zero, cosa que era inútil: MAX_VALUE
            // dóna el mateix resultat d'ajust amb una sola passada).
            Container parent = target.getParent();
            int tw = (parent instanceof JViewport) ? parent.getWidth()
                    : (parent != null ? parent.getWidth() : 0);
            if (tw == 0) tw = Integer.MAX_VALUE;
            int hgap = getHgap(), vgap = getVgap();
            Insets ins = target.getInsets();
            int maxW = tw - (ins.left + ins.right + hgap * 2);
            Dimension dim = new Dimension(0, 0);
            int rowW = 0, rowH = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowW + d.width > maxW) {
                    dim.width = Math.max(dim.width, rowW);
                    dim.height += rowH + vgap;
                    rowW = 0; rowH = 0;
                }
                if (rowW != 0) rowW += hgap;
                rowW += d.width;
                rowH = Math.max(rowH, d.height);
            }
            dim.width  = Math.max(dim.width, rowW);
            dim.height += rowH;
            dim.width  += ins.left + ins.right + hgap * 2;
            dim.height += ins.top  + ins.bottom + vgap * 2;
            return dim;
        }
    }
}
