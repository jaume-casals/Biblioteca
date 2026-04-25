# Database Security Audit

**Date:** 2026-04-25  
**Scope:** Java desktop app — `ServerConect.java`, `ControladorPersistencia.java`, `ControladorDomini.java`, `Config.java`  
**DB engines in scope:** H2 (embedded, default) / MariaDB (optional external)  
**Auditor:** Claude Code  

---

## Summary Risk Score: **5 / 10** (H2 default) / **6 / 10** (MariaDB) — DB-3 fixed

The live query layer is clean — nearly all runtime SQL uses `PreparedStatement` with no user-input concatenation. Risk concentrates in two places: (1) the SQL backup/restore pipeline uses string formatting with an incomplete escape function, and (2) `executeSQLFile` is an unrestricted SQL execution gate. Both are reachable from the UI.

---

## Top 5 Prioritized Fixes

| Priority | Finding | Effort | Risk Reduction |
|----------|---------|--------|----------------|
| 1 | DB-1 — whitelist `executeSQLFile` to INSERT/DELETE only | 10 min | Critical → Low |
| 2 | DB-2 — fix `sqlEsc` to also escape backslash | 2 min | High → Negligible |
| 3 | ~~DB-3 — pass MariaDB credentials via `Properties`, not URL~~ **FIXED** | 5 min | High → Low |
| 4 | DB-4 — wrap restore/multi-step writes in transactions (incl. edit delete+add) | 30 min | Medium → Low |
| 5 | DB-5 — mask password in `Config` save (at least warn on plaintext) | 20 min | Low → Low+ |

---

## Findings

---

### DB-1 — Unrestricted SQL File Execution Gate

**Severity:** Critical  
**CWE:** CWE-89 (SQL Injection), CWE-732 (Incorrect Permission Assignment for Critical Resource)  

**Evidence:** `ServerConect.java:392–411` — `executeSQLFile(File file)`

```java
// current code — only skips three statement types:
if (upper.startsWith("USE ") || upper.startsWith("CREATE DATABASE")
        || upper.startsWith("DROP DATABASE")) continue;
stmt.append(line).append(" ");
if (line.endsWith(";")) {
    con.createStatement().execute(stmt.toString().trim()); // ← arbitrary SQL
    stmt = new StringBuilder();
}
```

Called from `ControladorPersistencia.executeSQLFile` → `ControladorDomini.restoreFromSQL` → the UI "Restore Backup" dialog.

**Why it matters:**  
Any `.sql` file the user selects — including one crafted by a third party (e.g., a shared "book list") — executes verbatim against the live connection. With H2 embedded:

```sql
-- malicious_backup.sql
DROP TABLE llibre;
-- H2 SHUTDOWN drops the entire DB file:
SHUTDOWN;
```

With MariaDB (admin connection — see DB-3): `GRANT ALL ON *.* TO 'attacker'@'%'; FLUSH PRIVILEGES;`

**PoC:**
1. Craft a `.sql` with `SHUTDOWN;` (H2) or `DROP TABLE llibre;`
2. Open app → Restore → select file → click OK
3. Library data destroyed with no confirmation beyond file picker

**Remediation:**

```java
public synchronized void executeSQLFile(java.io.File file) throws Exception {
    try (java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
        StringBuilder stmt = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) continue;
            String upper = line.toUpperCase();
            if (upper.startsWith("USE ") || upper.startsWith("CREATE DATABASE")
                    || upper.startsWith("DROP DATABASE")) continue;
            stmt.append(line).append(" ");
            if (line.endsWith(";")) {
                String sql = stmt.toString().trim();
                String sqlUpper = sql.toUpperCase();
                // Whitelist: only INSERT and DELETE allowed during restore
                if (!sqlUpper.startsWith("INSERT ") && !sqlUpper.startsWith("DELETE ")) {
                    throw new Exception("Restore file contains disallowed statement: "
                        + sql.substring(0, Math.min(80, sql.length())));
                }
                con.createStatement().execute(sql);
                stmt = new StringBuilder();
            }
        }
    }
}
```

**Defense-in-depth:** Wrap `executeSQLFile` calls in a transaction so any error rolls back the entire restore atomically (see DB-4).

---

### DB-2 — `sqlEsc` Incomplete: Missing Backslash Escape

**Severity:** High  
**CWE:** CWE-89 (SQL Injection)  

**Evidence:** `ControladorDomini.java:255–257`

```java
private static String sqlEsc(String s) {
    return s == null ? "" : s.replace("'", "''");  // ← no backslash escape
}
```

Used in `backupToSQL` at lines 184–232 for every string field interpolated into `pw.printf(...)`.

**Why it matters:**  
H2 runs in `MODE=MySQL` (see `ServerConect.java:99`). In MySQL mode, backslash is an escape character. The sequence `\'` inside a string literal means an escaped quote — it does **not** close the string. Therefore:

```
Book title: foo\' OR 1=1 --
sqlEsc result: foo\' OR 1=1 --   (unchanged — no apostrophe to double)
Generated SQL: INSERT INTO llibre (...) VALUES (123,'foo\' OR 1=1 --',...);
```

The SQL parser sees `'foo\'` as an unclosed string + the rest as bare SQL. When this backup is restored, it injects arbitrary SQL into the live database.

**PoC:**
1. Add a book with title: `foo\', 0, 0, 0, 0, 0, true, ''); DROP TABLE tag; --`
2. App performs `autoBackup()` (daemon thread on startup) → writes malicious `.sql`
3. User later restores that backup → `DROP TABLE tag` executes

**Remediation:**

```java
private static String sqlEsc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")   // must be first
             .replace("'", "''");
}
```

**Defense-in-depth:** Consider replacing `backupToSQL`'s `printf` approach with `PreparedStatement` + `ResultSet` dump using the JDBC driver's own serialization, which removes the manual escaping requirement entirely. Alternatively, H2 exposes `SCRIPT TO 'path'` which generates a safe backup natively.

---

### DB-3 — MariaDB Password Embedded in JDBC URL ✅ FIXED 2026-04-25

**Severity:** High  
**CWE:** CWE-522 (Insufficiently Protected Credentials), CWE-312 (Cleartext Storage of Sensitive Information)  

**Evidence:** `ServerConect.java:102–105`

```java
String pw  = Config.getDbPassword();
String url = "jdbc:mariadb://" + Config.getDbHost() + "/?user=" + Config.getDbUser()
    + (pw.isEmpty() ? "" : "&password=" + pw);
con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client", url, null, null);
```

The H2 path immediately below correctly uses a `Properties` object:

```java
con = connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
// connectViaDriver puts user/password into java.util.Properties, NOT the URL
```

**Why it matters:**  
- `con.getMetaData().getURL()` returns the full URL including `&password=...` — logged in `getDbSizeBytes()` diagnostics.
- JDBC driver diagnostic logs, thread dumps, heap dumps, and exception messages can expose the URL string with embedded credentials.
- Java process listing (`ps aux`) on Linux shows JVM args but not environment; however, the password is loaded from a plaintext config file (see DB-5) which makes the URL construction redundant exposure on top of an already-weak credential store.

**Remediation:**

```java
// Replace lines 102–105 with:
java.util.Properties mariaProps = new java.util.Properties();
mariaProps.setProperty("user",     Config.getDbUser());
mariaProps.setProperty("password", Config.getDbPassword());
String url = "jdbc:mariadb://" + Config.getDbHost() + "/";
con = DriverManager.getConnection(url, mariaProps);
// ... then create DB and USE it as before
```

Or pass through `connectViaDriver`:

```java
con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
    "jdbc:mariadb://" + Config.getDbHost() + "/",
    Config.getDbUser(), Config.getDbPassword());
```

`connectViaDriver` already builds a `Properties` object correctly — just pass non-null user/password.

---

### DB-4 — Multi-Step Writes Not Wrapped in Transactions

**Severity:** Medium  
**CWE:** CWE-362 (TOCTOU / Race), CWE-20 (Incomplete Error Handling)  

**Evidence:**

| Operation | Files involved | Risk |
|-----------|---------------|------|
| `afegirLlibre` + `syncAutors` | `ServerConect.java:286–340` | Book inserted, then autor link INSERT fails → book orphaned with no author link |
| `restoreFromSQL` (8 DELETEs + N INSERTs) | `ServerConect.java:353–362`, `392–411` | Partial restore if any statement throws; DB left partially cleared |
| `clearAllData` (8 DELETEs) | `ServerConect.java:353–362` | One DELETE fails → FK-inconsistent state |
| `replaceAllLlibres` (`resetDatabase` + loop) | `ControladorPersistencia.java:38–41` | DB wiped, then first INSERT fails → empty library |
| `swapLlistesOrdre` (2N+2 UPDATEs) | `ControladorDomini.java:313–325` | Partial reorder leaves duplicate or inconsistent `ordre` values |
| `editar` (delete old + add new) | `DetallesLlibrePanelControl.java:197–198` | `deleteLlibre(old)` succeeds, then `addLlibre(new)` throws → book permanently lost with no rollback |

**Why it matters:**  
Concurrent disc I/O errors, H2 lock contention, or a bug in later statements leave the database in a partially-mutated state that is invisible to the user and non-recoverable without a backup.

**Remediation — minimal, drop-in pattern:**

```java
// In ServerConect — wrap multi-step methods:
public synchronized void afegirLlibre(Llibre llibre) throws SQLException {
    con.setAutoCommit(false);
    try {
        // ... existing PreparedStatement code ...
        if (!llibre.getAutors().isEmpty()) syncAutors(llibre.getISBN(), llibre.getAutors());
        con.commit();
    } catch (SQLException e) {
        con.rollback();
        throw e;
    } finally {
        con.setAutoCommit(true);
    }
}
```

Same pattern for `clearAllData`, `executeSQLFile`, and `replaceAllLlibres`.

For `executeSQLFile` specifically — a single transaction wrapping all restore statements also defends against the DB-1 attack (partial execution would roll back on error even before the whitelist fix).

---

### DB-5 — Database Password Stored Plaintext in Config File

**Severity:** Medium (MariaDB only; H2 has no real password)  
**CWE:** CWE-256 (Unprotected Storage of Credentials), CWE-312  

**Evidence:** `Config.java:41–42`

```java
public static String getDbPassword() { return props.getProperty("dbPassword", ""); }
public static void setDbPassword(String pw) { props.setProperty("dbPassword", pw); save(); }
```

Written to `~/.biblioteca/config.properties` (plaintext) by `Config.save()`.

**Why it matters:**  
Any process running as the same user can read `~/.biblioteca/config.properties`. File permissions are not set by the app — they inherit the user's `umask` (typically 0644, world-readable). A malicious process or a backup tool that copies `~/.biblioteca/` exposes the MariaDB password.

**Remediation:**

Minimal: restrict file permissions on write.

```java
private static void save() {
    try {
        FILE.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            props.store(out, "Biblioteca configuration");
        }
        // Restrict to owner read/write only (rw-------)
        FILE.setReadable(false, false);
        FILE.setWritable(false, false);
        FILE.setReadable(true, true);
        FILE.setWritable(true, true);
    } catch (IOException ignored) {}
}
```

**Defense-in-depth:** For a personal app, this is proportionate. For a shared or production deployment, use OS keyring (e.g., `java.security.KeyStore`, `libsecret` via JNI, or store credentials in an environment variable).

---

### DB-6 — No Query Timeouts Configured

**Severity:** Low  
**CWE:** CWE-400 (Uncontrolled Resource Consumption)  

**Evidence:** No `setQueryTimeout()` call anywhere in `ServerConect.java`. No JDBC URL `socketTimeout` or `connectTimeout` parameter.

**Why it matters:**  
For H2 embedded this is low-risk (single process). For MariaDB, a network partition or server hang blocks the synchronized `ServerConect` methods indefinitely, freezing the Swing EDT (all methods are `synchronized`, and `getAllLlibres` is called on the main thread before the EDT starts — `Ejecutable.java:38`).

**Remediation:**

```java
// Add to connectViaDriver props for MariaDB:
props.setProperty("connectTimeout", "5000");   // 5s
props.setProperty("socketTimeout",  "30000");  // 30s per statement

// And in long-running Statement calls:
Statement stmt = con.createStatement();
stmt.setQueryTimeout(30); // seconds
```

---

### DB-7 — No Audit Logging for Sensitive Operations

**Severity:** Low  
**CWE:** CWE-778 (Insufficient Logging)  

**Evidence:** No logging infrastructure in `ServerConect.java` or `ControladorDomini.java`. `clearAllData()`, `resetDatabase()`, `executeSQLFile()`, `backupToSQL()`, and `restoreFromSQL()` fire silently.

**Why it matters:**  
For a personal single-user desktop app, this is informational. If the app is ever deployed in a shared or institutional context, there is no record of who deleted or restored the library, or what SQL files were imported.

**Remediation:**  
Add a simple `java.util.logging.Logger` at `INFO` level for destructive ops:

```java
private static final java.util.logging.Logger LOG =
    java.util.logging.Logger.getLogger(ServerConect.class.getName());

// In clearAllData():
LOG.warning("clearAllData() called — all rows will be deleted");
// In executeSQLFile():
LOG.info("executeSQLFile: " + file.getAbsolutePath());
```

---

### DB-8 — Backup Files Stored Unencrypted on Disk

**Severity:** Low  
**CWE:** CWE-311 (Missing Encryption of Sensitive Data)  

**Evidence:** `ControladorDomini.java:155–160`

```java
backupToSQL(new java.io.File(dir, "biblioteca_" + ts + ".sql"));
```

Writes to `~/.biblioteca/backups/`. SQL dumps contain all book data: notes (`VARCHAR(2048)`), purchase dates (`data_compra`), reading dates, personal ratings, loan records with `nom_persona`.

**Why it matters:**  
Backup files may be included in unencrypted system backups, cloud sync (iCloud/OneDrive/Dropbox syncing `~`), or accessed by other processes. `nom_persona` in `prestec` is PII (loan borrower names).

**Remediation:**  
Minimal: apply same file permission restriction as DB-5. Full: encrypt with a user-derived key or use H2's built-in encrypted file format (`CIPHER=AES` in H2 URL). H2 native backup via `BACKUP TO 'path.zip'` also creates a compressed, permissions-restricted copy.

---

### DB-10 — `resetDatabase` Drops Only `llibre` Table in H2 Mode

**Severity:** Medium (data integrity, triggered by "replace all" import flow)  
**CWE:** CWE-459 (Incomplete Cleanup)

**Evidence:** `ServerConect.java:376–390`

```java
public synchronized void resetDatabase() {
    try {
        Statement s = con.createStatement();
        if ("h2".equals(Config.getDbType())) {
            s.executeUpdate("DROP TABLE IF EXISTS llibre");   // ← only one table
        } else {
            s.executeUpdate("DROP DATABASE IF EXISTS `BIBLIOTECA`");
            // ...
        }
        s.executeUpdate(CREATE_TABLE); // recreate llibre only
    }
```

Called from `ControladorPersistencia.replaceAllLlibres` before bulk-importing books.

**Why it matters:**  
After `DROP TABLE llibre` + `CREATE TABLE llibre`:
- `prestec`, `llibre_llista`, `llibre_autor`, `llibre_tag` still exist with their old rows.
- New `llibre` rows get fresh ISBNs; old FK rows now reference non-existent ISBNs.
- H2's FK enforcement will fire on the next INSERT that tries to match those orphan FKs, or silently leave ghost rows that corrupt shelf/tag/loan queries.
- Cascading deletes were attached to the old table object; recreating `llibre` does not re-establish them on the FK side tables.

**PoC:**
1. Add book A to shelf S.
2. Import a new library (triggers `replaceAllLlibres`).
3. `llibre` is dropped and recreated; `llibre_llista` still has a row for A's ISBN.
4. `getAllLlibreLlista()` returns orphan rows referencing books that no longer exist.

**Remediation:**

```java
if ("h2".equals(Config.getDbType())) {
    // Drop all tables in FK-safe order (children before parents)
    for (String t : new String[]{
            "prestec","llibre_llista","llibre_autor","llibre_tag",
            "llista","autor","tag","schema_version","llibre"}) {
        s.executeUpdate("DROP TABLE IF EXISTS " + t);
    }
}
```

Then let `runMigrations()` recreate everything from scratch. Alternatively call `clearAllData()` (which already does the ordered DELETEs) instead of dropping tables.

---

### DB-9 — `clearAllData` / `resetDatabase` Called Without Application-Level Authorization

**Severity:** Low (internal only)  

**Evidence:** `ControladorPersistencia.replaceAllLlibres` calls `sc.resetDatabase()` (drops/recreates table) with no second check beyond the UI confirmation dialog. `clearAllData()` is exposed publicly on `ControladorPersistencia` and callable from anywhere in the domain layer.

**Why it matters:**  
Any future code path that reaches `clearAllData()` or `replaceAllLlibres()` without going through the UI dialog bypasses the only safety gate. Both methods operate at the persistence layer with no guard.

**Remediation:**  
Add `@Deprecated` or a package-private visibility modifier so domain code cannot call it directly, and document that the UI dialog is the only intended caller.

---

## Checklist Diff

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | Parameterized queries / ORM | **PASS** (with exception) | All live queries use `PreparedStatement`. `backupToSQL` uses `printf` — flagged in DB-2 |
| 2 | Connection string security | **PASS** ✅ | DB-3 fixed: credentials now passed via `Properties`, not URL |
| 3 | DB user permissions (least privilege) | **Unable to verify** | No DB user creation in codebase. MariaDB user configured externally. H2 uses `sa` (superuser) by design for embedded. |
| 4 | Sensitive data encryption at rest | **FAIL** | Config file plaintext (DB-5), backups unencrypted (DB-8) |
| 5 | PII handling compliance | **FAIL** | `nom_persona` in `prestec` is PII; no retention policy, no deletion workflow, no redaction in logs |
| 6 | Query timeout configurations | **FAIL** | No `setQueryTimeout`, no URL timeout params (DB-6) |
| 7 | Connection pool settings | **N/A** | Single-user embedded app; single `Connection` with `synchronized` methods is appropriate for H2 |
| 8 | Transaction handling | **FAIL** | Multi-step writes not atomic (DB-4); delete+add edit path in `DetallesLlibrePanelControl.java:197–198` can lose a book permanently on failure |
| 9 | Audit logging | **FAIL** | No logging of destructive ops (DB-7) |
| 10 | NoSQL injection | **N/A** | No NoSQL used |
| 11 | Row/tenant isolation | **N/A** | Single-user app |
| 12 | Least-privilege networking | **N/A** for H2 / **Unable to verify** for MariaDB | H2 is embedded (no network). MariaDB network config is external to the app. |
| 13 | TLS in transit | **N/A** for H2 / **FAIL** for MariaDB | No `useSSL=true` in MariaDB URL. For localhost deployments this is low-risk. |
| 14 | Secret management & rotation | **FAIL** | Password in plaintext config (DB-5); no rotation mechanism |
| 15 | Schema & integrity controls | **PASS** | FK with `ON DELETE CASCADE`, `UNIQUE`, `NOT NULL` enforced throughout. No `CHECK` constraints on value ranges, but acceptable for this domain. |
| 16 | Field-level minimization | **PASS** | No `SELECT *`. `getAllLlibres` enumerates columns explicitly. BLOB loaded on-demand via `getLlibreBlob`. |
| 17 | Pagination & query limits | **PASS** | `getRecentlyAdded` uses `LIMIT ?` with `PreparedStatement`. In-memory filtering elsewhere. |
| 18 | Backup/restore security | **FAIL** | Backup unencrypted (DB-8); restore executes arbitrary SQL (DB-1) |
| 19 | Data retention & deletion | **FAIL** | No retention policy; `nom_persona` accumulates indefinitely; no secure erasure |
| 20 | Migrations safety | **PASS** (minor concern) | Migrations run via app connection. No destructive DDL. Out-of-order version 13 in array is known and benign (see code_facts). |
| 21 | ORM raw-query escape hatch review | **N/A** | No ORM; `Statement` used only for static queries with no user input |
| 22 | LIKE / regex input handling | **PASS** | All user-input filtering done in-memory (Java), not in SQL. No dynamic `LIKE` clauses. |
| 23 | Query timeouts & resource guards | **FAIL** | No timeouts (DB-6) |
| 24 | Audit & monitoring depth | **FAIL** | No logging infrastructure (DB-7) |
| 25 | PII in logs/metrics | **PASS** | No logging at all (also why DB-7 is a finding) |
| 26 | Indexing of sensitive data | **N/A** | No SSNs or tokens. `nom_persona` in `prestec` not indexed. |
| 27 | Service/account lifecycle | **Unable to verify** | MariaDB user management is external. H2 uses fixed `sa`. |
| 28 | Caching layers | **N/A** | No Redis/Memcached. In-memory `ArrayList<Llibre>` is the only cache; it contains the same data already in the DB file. |
| 29 | Analytics/ETL exports | **N/A** | No ETL pipeline |

---

## Appendix: H2 MySQL-mode Backslash Behavior

H2 in `MODE=MySQL` (enabled via `ServerConect.java:99`) treats `\` as an escape character in string literals, matching MySQL behavior. This means:

- `'\''` = single quote character (standard SQL doubling — works)
- `'\\'` = literal backslash (MySQL escape)
- `\''` = escaped-quote followed by empty-string close = **closes the string at the first `\''`**

Example of how DB-2 attack reaches injection:

```
Input title:  O\'Reilly
After sqlEsc: O\'Reilly          ← backslash not escaped
In SQL:       '..., 'O\'Reilly', ...'
H2 parse:     '..., 'O\''         ← string closes here
              Reilly, ...'        ← interpreted as identifier/expression
```

Fix: `s.replace("\\", "\\\\").replace("'", "''")` — order matters; backslash must be replaced first.
