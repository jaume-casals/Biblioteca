package presentacio.config;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JDialog;
import javax.swing.JPanel;

/** Contract for a settings dialog section: build UI and reload from config. */
public interface ConfiguracioSeccio {
    JPanel build(JDialog owner);

    void reloadFromConfig(JPanel root);

    static ConfiguracioSeccio of(Function<JDialog, JPanel> build, Consumer<JPanel> reload) {
        return new ConfiguracioSeccio() {
            @Override public JPanel build(JDialog owner) { return build.apply(owner); }
            @Override public void reloadFromConfig(JPanel root) { reload.accept(root); }
        };
    }
}
