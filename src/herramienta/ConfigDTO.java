package herramienta;

/**
 * DTO for the Swing settings dialog. Mirrors fields exposed in configuration UI;
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
            UiConfig.setTheme(UITheme.Theme.fromKey(theme));
            UiConfig.setDarkMode(darkMode);
            DbConfig.setType(dbType);
            DbConfig.setHost(dbHost);
            DbConfig.setUser(dbUser);
            if (!"***".equals(dbPassword)) DbConfig.setPassword(dbPassword);
            UiConfig.setFontSize(fontSize);
            UiConfig.setCurrency(currencySymbol);
            UiConfig.setDefaultValoracio(defaultValoracio);
            UiConfig.setReadingGoal(readingGoal);
            UiConfig.setViewMode(viewMode);
            UiConfig.setGalleryZoom(galleryZoom);
            FilterConfig.setDefaultImgDir(defaultImgDir);
        });
    }
}
