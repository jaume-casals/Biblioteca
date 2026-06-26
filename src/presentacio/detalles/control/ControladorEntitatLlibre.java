package presentacio.detalles.control;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import domini.Llibre;
import herramienta.ui.DialegError;
import herramienta.i18n.I18n;

abstract class ControladorEntitatLlibre<V, W> {

    protected final V vista;
    protected final Llibre llibre;
    protected final W cd;

    protected ControladorEntitatLlibre(V vista, Llibre llibre, W cd) {
        this.vista = vista;
        this.llibre = llibre;
        this.cd = cd;
    }

    protected final void initEntitatLlibre() {
        wireListeners();
        reload();
    }

    protected abstract void wireListeners();
    protected abstract void reload();
    protected abstract Component obtenirParentDialeg();
    protected abstract JComboBox<?> obtenirComboAdd();

    protected boolean comboBuit(String msgKey, String titleKey) {
        if (obtenirComboAdd().getItemCount() == 0) {
            JOptionPane.showMessageDialog(obtenirParentDialeg(),
                    I18n.t(msgKey), I18n.t(titleKey), JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    protected <D> void executeReload(java.util.concurrent.Callable<D> background,
                                     java.util.function.Consumer<D> onSuccess) {
        new SwingWorker<D, Void>() {
            @Override protected D doInBackground() throws Exception {
                return background.call();
            }
            @Override protected void done() {
                if (isCancelled()) return;
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    new DialegError(ex).mostrarErrorMessage();
                }
            }
        }.execute();
    }
}
