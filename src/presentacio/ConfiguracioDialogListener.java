package presentacio;

/** Callbacks fired by the settings dialog. Replaces raw Runnable parameters. */
public interface ConfiguracioDialogListener {
    default void onThemeChange()   {}
    default void onProfileSwitch() {}
    default void onFontChange()    {}
    default void onRefreshData()   {}
}
