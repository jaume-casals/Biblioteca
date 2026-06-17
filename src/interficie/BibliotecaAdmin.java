package interficie;

import java.io.File;

/**
 * Danger-zone administrative operations.
 * Separate sub-interface so consumers that only need reads/writes (UI, API) need not depend
 * on backup/restore/clear capabilities.
 */
public interface BibliotecaAdmin {
    void copiaSegToSQL(File f) throws Exception;
    void restaurarFromSQL(File f) throws Exception;
    void netejarAll() throws Exception;
}
