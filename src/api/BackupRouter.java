package api;

import domini.ControladorDomini;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;

public class BackupRouter {

    private final ControladorDomini cd = ControladorDomini.getInstance();

    public BackupRouter(HttpRouter app) {
        app.post("/api/backup",  ctx -> backup(ctx));
        app.post("/api/restore", ctx -> restore(ctx));
        app.post("/api/clear",   ctx -> clear(ctx));
    }

    private void backup(HttpCtx ctx) throws Exception {
        File dir = new File(System.getProperty("user.home"), ".biblioteca/backups");
        dir.mkdirs();
        File out = new File(dir, "biblioteca_" + LocalDate.now() + ".sql");
        synchronized (cd) { cd.backupToSQL(out); }
        ctx.json(Map.of("file", out.getAbsolutePath(), "ok", true));
    }

    private void restore(HttpCtx ctx) throws Exception {
        byte[] data = ctx.bodyBytes();
        if (data.length == 0) throw new Exception("Empty SQL body");
        File tmp = File.createTempFile("biblioteca_restore_", ".sql");
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), data);
        synchronized (cd) { cd.restoreFromSQL(tmp); }
        tmp.delete();
        ctx.json(Map.of("ok", true));
    }

    private void clear(HttpCtx ctx) throws Exception {
        synchronized (cd) { cd.clearAll(); }
        ctx.json(Map.of("ok", true));
    }
}
