package api;

import herramienta.Config;
import interficie.BibliotecaWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BackupRouter {

    private final BibliotecaWriter cd;

    public BackupRouter(HttpRouter app, BibliotecaWriter cd) {
        this.cd = cd;
        app.get("/api/backups",  ctx -> listBackups(ctx));
        app.post("/api/backup",  ctx -> backup(ctx));
        app.post("/api/restore", ctx -> restore(ctx));
        app.post("/api/clear",   ctx -> clear(ctx));
    }

    private void listBackups(HttpCtx ctx) {
        File dir = Config.getBackupDir();
        List<Map<String, Object>> result = new ArrayList<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
            if (files != null) {
                Arrays.sort(files, java.util.Comparator.comparing(File::getName).reversed());
                for (File f : files)
                    result.add(Map.of("name", f.getName(), "path", f.getAbsolutePath(), "size", f.length()));
            }
        }
        ctx.json(Map.of("backups", result));
    }

    private void backup(HttpCtx ctx) throws Exception {
        File dir = Config.getBackupDir();
        if (!dir.mkdirs() && !dir.isDirectory()) throw new Exception("Cannot create backup directory: " + dir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File out = new File(dir, "biblioteca_" + ts + ".sql");
        synchronized (cd) { cd.backupToSQL(out); }
        ctx.json(Map.of("file", out.getAbsolutePath(), "ok", true));
    }

    private void restore(HttpCtx ctx) throws Exception {
        byte[] data = ctx.bodyBytes();
        if (data.length == 0) throw new IllegalArgumentException("Empty SQL body");
        File tmp = File.createTempFile("biblioteca_restore_", ".sql");
        tmp.deleteOnExit();
        try {
            Files.write(tmp.toPath(), data);
            synchronized (cd) { cd.restoreFromSQL(tmp); }
        } finally {
            try { java.nio.file.Files.deleteIfExists(tmp.toPath()); }
            catch (IOException e) { System.err.println("Failed to delete restore temp file: " + tmp); }
        }
        ctx.json(Map.of("ok", true));
    }

    private void clear(HttpCtx ctx) throws Exception {
        File dir = Config.getBackupDir();
        dir.mkdirs();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File out = new File(dir, "pre_clear_" + ts + ".sql");
        synchronized (cd) {
            cd.backupToSQL(out);
            cd.clearAll();
        }
        ctx.json(Map.of("ok", true, "backup", out.getAbsolutePath()));
    }
}
