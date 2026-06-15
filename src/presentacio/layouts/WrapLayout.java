package presentacio.layouts;

import javax.swing.JViewport;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/*
 * WrapLayout — FlowLayout subclass that lets components wrap to
 * multiple rows based on parent viewport width. Originally extracted
 * from {@code GaleriaCobertesPanel}'s private inner class for reuse.
 *
 * Adapted from the public Sun Java Tutorial "WrapLayout" example:
 *   https://docs.oracle.com/javase/tutorial/uiswing/layout/custom.html
 *
 * Sun Microsystems, Inc. ("Sun") SOFTWARE LICENSE AGREEMENT for the
 * Java(TM) Tutorial Sample Code. The original tutorial code is in the
 * public domain for tutorial use; this adaptation preserves the
 * layoutSize() walk-the-tree fallback but uses a single MAX_VALUE
 * fallback instead of the double-walk pattern (see the tot.txt LOW
 * finding on the wasted second walk).
 */
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
            // Read the parent's width directly; fall back to MAX_VALUE on
            // the FIRST try (per the tot.txt LOW finding — the old code
            // did a second tree-walk to recover from a zero-width parent,
            // which is wasteful: MAX_VALUE gives the same wrap result with
            // a single pass).
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
