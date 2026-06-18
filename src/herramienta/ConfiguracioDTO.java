package herramienta;

/**
 * DTO per al diàleg de configuració Swing. Reflecteix els camps exposats a
 * la IU de configuració; l'estat persistent continua vivint a {@link Configuracio}.
 */
public record ConfiguracioDTO(
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
    public static ConfiguracioDTO fromConfig() {
        return new ConfiguracioDTO(
            Configuracio.obtenirTheme().key(),
            Configuracio.esDarkMode(),
            Configuracio.obtenirDbType(), Configuracio.obtenirDbHost(), Configuracio.obtenirDbUser(), Configuracio.obtenirDbPassword(),
            Configuracio.obtenirFontSize(),
            Configuracio.getCurrencySymbol(),
            Configuracio.obtenirDefaultValoracio(),
            Configuracio.obtenirReadingGoal(),
            Configuracio.obtenirViewMode(), Configuracio.obtenirGalleryZoom(),
            Configuracio.obtenirDefaultImgDir()
        );
    }

    public void apply() {
        Configuracio.withBatch(() -> {
            ConfiguracioUi.posarTheme(UITheme.Tema.fromKey(theme));
            ConfiguracioUi.posarDarkMode(darkMode);
            ConfiguracioDb.setType(dbType);
            ConfiguracioDb.posarHost(dbHost);
            ConfiguracioDb.posarUser(dbUser);
            if (!"***".equals(dbPassword)) ConfiguracioDb.posarPassword(dbPassword);
            ConfiguracioUi.posarFontSize(fontSize);
            ConfiguracioUi.setCurrency(currencySymbol);
            ConfiguracioUi.posarDefaultValoracio(defaultValoracio);
            ConfiguracioUi.posarReadingGoal(readingGoal);
            ConfiguracioUi.posarViewMode(viewMode);
            ConfiguracioUi.posarGalleryZoom(galleryZoom);
            ConfiguracioFiltre.posarDefaultImgDir(defaultImgDir);
        });
    }
}
