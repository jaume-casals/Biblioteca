package presentacio.layouts;

import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * FlowLayout subclass that lets components wrap to multiple rows based on parent viewport width.
 * Extracted from {@code GaleriaCobertesPanel}'s private inner class for reuse.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container t) { return layoutSize(t, true);  }
    @Override public Dimension minimumLayoutSize(Container t)   { return layoutSize(t, false); }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Container parent = target.getParent();
            int tw = (parent instanceof JViewport) ? parent.getWidth() : 0;
            if (tw == 0) {
                Container c = target;
                while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
                tw = c.getSize().width;
            }
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
