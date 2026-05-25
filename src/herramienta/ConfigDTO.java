package herramienta;

/**
 * Shared DTO for shipping configuration between the Swing settings dialog and the
 * {@code /api/config} HTTP endpoint. The record only mirrors fields exposed in both surfaces;
 * persistent state still lives in {@link Config}.
 */
public record ConfigDTO(
    String theme,
    boolean darkMode,
    String dbType, String dbHost, String dbUser, String dbPassword,
    String fontSize,
    String currencySymbol,
    double defaultValoracio,
    int readingGoal,
    String viewMode, int galleryZoom,
    String defaultImgDir
) {
    public static ConfigDTO fromConfig() {
        return new ConfigDTO(
            Config.getTheme().key(),
            Config.isDarkMode(),
            Config.getDbType(), Config.getDbHost(), Config.getDbUser(), Config.getDbPassword(),
            Config.getFontSize(),
            Config.getCurrencySymbol(),
            Config.getDefaultValoracio(),
            Config.getReadingGoal(),
            Config.getViewMode(), Config.getGalleryZoom(),
            Config.getDefaultImgDir()
        );
    }

    public void apply() {
        Config.withBatch(() -> {
            Config.setTheme(UITheme.Theme.fromKey(theme));
            Config.setDarkMode(darkMode);
            Config.setDbType(dbType);
            Config.setDbHost(dbHost);
            Config.setDbUser(dbUser);
            if (!"***".equals(dbPassword)) Config.setDbPassword(dbPassword);
            Config.setFontSize(fontSize);
            Config.setCurrencySymbol(currencySymbol);
            Config.setDefaultValoracio(defaultValoracio);
            Config.setReadingGoal(readingGoal);
            Config.setViewMode(viewMode);
            Config.setGalleryZoom(galleryZoom);
            Config.setDefaultImgDir(defaultImgDir);
        });
    }
}
