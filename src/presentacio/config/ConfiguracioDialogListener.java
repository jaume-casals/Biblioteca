package presentacio.config;

/** Callbacks que dispara el diàleg de configuració. Substitueix els paràmetres Runnable en brut. */
public interface ConfiguracioDialogListener {
    default void enCanviarTema()       {}
    default void enCanviarPerfil()     {}
    default void enCanviarLletra()     {}
    default void enRefrescarDades()    {}
}
