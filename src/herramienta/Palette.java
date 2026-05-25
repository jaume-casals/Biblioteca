package herramienta;

import java.awt.Color;

/**
 * Snapshot of all theme-dependent colors. Future refactor: bind one Palette per {@code Theme}
 * enum value and replace the large switch in {@link UITheme#setTheme} with a {@code palette.apply()}.
 */
public record Palette(
    Color bgMain, Color bgPanel, Color accent, Color accentAlt,
    Color textDark, Color textMid, Color borderClr,
    Color headerBg, Color headerFg, Color tableGrid, Color tableAlt,
    Color sidebarBg, Color sidebarAccent, Color sidebarText, Color sidebarTextMid, Color sidebarSelBg
) {}
