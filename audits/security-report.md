# Input Validation & Security Audit
_Generated 2026-04-25 — Biblioteca project_

> **Context:** Local desktop Swing application. Single user. No network listener, no web server.  
> This narrows the attack surface significantly: most web-centric categories are N/A.  
> Remaining risks are data integrity, credential exposure, and crafted-file attacks.

---

## Summary Risk Score: **4 / 10**

Low-to-medium overall because the application is local and single-user.  
Two issues are High severity in an absolute sense but reduced to Medium in context.  
One issue (MariaDB credential) reaches High if MariaDB mode is used with a real server.

**Top 3 fixes that reduce risk fastest:**

| # | Fix | Effort | Risk drop |
|---|-----|--------|-----------|
| 1 | Replace `sqlEsc` with parameterized export in `backupToSQL` | 2 h | SQL injection in backup eliminated |
| 2 | Pass MariaDB creds via `Properties`, not URL | 10 min | Credential exposure eliminated |
| 3 | Validate + normalise image path before `Files.readAllBytes` | 20 min | File read confined to expected dirs |

---

## Checklist

| Category | Status | Notes |
|---|---|---|
| SQL Injection | **FAIL** | `backupToSQL` uses `printf`+`sqlEsc`; insufficient in H2 MySQL mode |
| NoSQL Injection | N/A | App uses SQL only |
| Command Injection | **PASS** | No `Runtime.exec()`, `ProcessBuilder`, or shell invocation anywhere |
| XSS Prevention | N/A | Swing desktop app; no HTML response generation |
| XXE | N/A | No XML parsing in codebase |
| Path Traversal | **FAIL** | Image path field read directly via `Files.readAllBytes(Path.of(userInput))` |
| URL parameter injection | **FAIL** | `lookupByISBN` concatenates ISBN into URL without encoding |
| Request / field validation | **PARTIAL** | `LlibreValidator` covers ISBN/nom/valoracio/preu; no length limits, no date format check |
| Credential storage | **FAIL** | MariaDB password embedded in JDBC URL string and stored plaintext in config |
| Arbitrary SQL exec | **FAIL** | `executeSQLFile` executes every statement in a user-selected file verbatim |

---

## Findings

---

### SEC-1 — SQL Injection via backslash in `backupToSQL` (MySQL mode)
**Severity: Medium** | CWE-89

**Evidence:**

```java
// ControladorDomini.java:183-204
pw.printf(
    "INSERT INTO llibre (...) VALUES (%d,'%s','%s',%d,'%s',%.4f,%.4f,...);%n",
    l.getISBN(),
    sqlEsc(l.getNom()),      // ← string-formatted, not parameterized
    sqlEsc(l.getAutor()),
    ...

// ControladorDomini.java:255
private static String sqlEsc(String s) {
    return s == null ? "" : s.replace("'", "''");  // ← only escapes single-quote
}
```

H2 URL (ServerConect.java:99):
```
jdbc:h2:.../biblioteca;MODE=MySQL;...
```

In H2 MySQL mode, backslash (`\`) is a string escape character. `sqlEsc` does not escape backslashes.

**Why it matters:**  
An attacker who can control a field value can inject a `\` + `'` sequence. After `sqlEsc` doubles the `'`, the resulting `\''` in MySQL mode parses as: `\'` = escaped literal `'` (string continues), then `'` = close string. Content after that executes as raw SQL in the generated backup file.

**PoC (generates malformed/injected backup SQL):**

1. Create a book with:
   - nom = `x\\', 0, 0, 0, 0, false, '', '', 0, 0, '', '', 0, NULL, NULL, NULL, NULL, false); DROP TABLE tag; --`
2. Run Backup BD → save `backup.sql`
3. Open `backup.sql` — the nom INSERT becomes:
   ```sql
   INSERT INTO llibre (...) VALUES (isbn,'x\'',...);
   ```
   In MySQL mode: `'x\''` = string `x'` (closed), then `, 0, 0, ...); DROP TABLE tag; --` executes when backup is restored.
4. `restoreFromSQL` → `executeSQLFile` executes the injected `DROP TABLE tag` statement.

This attack is viable via CSV import → backup → share (victim restores malicious backup).

**Remediation:**

Option A — also escape backslashes in `sqlEsc`:

```java
// ControladorDomini.java:255
private static String sqlEsc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")   // escape backslash FIRST
             .replace("'",  "''");   // then escape single-quote
}
```

Option B (preferred) — generate backup using `PreparedStatement` into a temp table then export, OR use H2's own `SCRIPT` command:

```java
// ControladorDomini.java — replace manual printf with H2 SCRIPT TO FILE
public void backupToSQL(java.io.File file) throws Exception {
    // H2 SCRIPT produces correct, injection-safe output
    cp.executeSqlStatement("SCRIPT NODATA DROP TO '" + file.getAbsolutePath().replace("\\", "/") + "'");
    // Note: H2 SCRIPT doesn't export BLOB data; keep manual blob export separate
}
```

If manual printf must remain, escaping backslash (Option A) is the minimal fix. Add a test:

```java
test("backupToSQL escapes backslash in nom", () -> {
    resetSingletons();
    ControladorDomini cd = ControladorDomini.getInstance();
    cd.addLlibre(LlibreValidator.checkLlibre(9780306406157L, "A\\' test", null, null, null, null, null, null, null));
    java.io.File f = java.io.File.createTempFile("sec_esc_", ".sql");
    f.deleteOnExit();
    cd.backupToSQL(f);
    cd.clearAll();
    cd.restoreFromSQL(f);
    assertEqual("A\\' test", cd.getLlibre(9780306406157L).getNom());
});
```

**Defense-in-depth:**  
`executeSQLFile` (SEC-3) should additionally validate that restored files contain only the expected statement types.

---

### SEC-2 — MariaDB credentials embedded in JDBC URL
**Severity: High** (if MariaDB used) / **Low** (H2-only deployments) | CWE-256, CWE-312

**Evidence:**

```java
// ServerConect.java:102-106
String pw  = Config.getDbPassword();
String url = "jdbc:mariadb://" + Config.getDbHost() + "/?user=" + Config.getDbUser()
    + (pw.isEmpty() ? "" : "&password=" + pw);
con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client", url, null, null);
```

The password is in the URL string, not in a `Properties` object. The same `connectViaDriver` already uses Properties correctly for H2 (lines 153-156):

```java
// ServerConect.java:153 — H2 uses Properties correctly
java.util.Properties props = new java.util.Properties();
if (user != null)     props.setProperty("user",     user);
if (password != null) props.setProperty("password", password);
```

But for MariaDB, `null, null` is passed to `connectViaDriver` and credentials go in the URL.

**Why it matters:**  
URL strings appear in: exception stack traces (printed to stderr), JVM heap dumps, thread dumps (VisualVM/JConsole), JDBC driver debug logs, OS process lists (`/proc/*/cmdline` does not show this but JVM args might), and `~/.biblioteca/config.properties` in plaintext.

**Remediation:**

```java
// ServerConect.java:102-106 — replace with:
String user = Config.getDbUser();
String pw   = Config.getDbPassword();
String url  = "jdbc:mariadb://" + Config.getDbHost() + "/BIBLIOTECA";
con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client", url, user,
        pw.isEmpty() ? null : pw);
```

`connectViaDriver` already places non-null user/password into `Properties` (line 153-155). This one-line URL change moves credentials out of the URL string entirely.

**Config file plaintext storage (secondary issue):**  
`Config.java:42` stores/retrieves `dbPassword` as plaintext. For a personal desktop app this is acceptable, but document it. Minimum: ensure `~/.biblioteca/` has `0700` permissions:

```java
// Config.java — add after mkdirs():
new File(System.getProperty("user.home") + "/.biblioteca").setReadable(false, false);
new File(System.getProperty("user.home") + "/.biblioteca").setReadable(true, true);  // owner only
new File(System.getProperty("user.home") + "/.biblioteca").setExecutable(true, true);
new File(System.getProperty("user.home") + "/.biblioteca").setWritable(true, true);
```

Or use `PosixFilePermissions` for cleaner code.

---

### SEC-3 — `executeSQLFile` executes arbitrary SQL verbatim
**Severity: Medium** | CWE-89 (second-order)

**Evidence:**

```java
// ServerConect.java:392-411
public synchronized void executeSQLFile(java.io.File file) throws Exception {
    try (java.io.BufferedReader br = ...) {
        StringBuilder stmt = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("--")) continue;
            ...
            if (line.endsWith(";")) {
                con.createStatement().execute(stmt.toString().trim());  // ← raw exec
                stmt = new StringBuilder();
            }
        }
    }
}
```

**Why it matters:**  
Any SQL file the user restores executes without validation. If a user:
1. Receives a "backup" file from an untrusted source and restores it, **all SQL executes**, including `DROP TABLE`, `CREATE USER` (MariaDB), `CALL`, `EXECUTE`, etc.
2. The exploit from SEC-1 lands here — injected statements in a malicious backup execute on restore.

**PoC:**  
Create a file `malicious.sql` containing:
```sql
DELETE FROM llibre;
DELETE FROM llista;
```
User selects it as restore file → all data is erased.

On MariaDB with a high-privilege user: `CREATE USER 'backdoor'@'%' IDENTIFIED BY 'password';` would succeed.

**Remediation:**  

Whitelist accepted statement types. Add a pre-execution validator:

```java
// ServerConect.java — add before con.createStatement().execute():
private static final java.util.regex.Pattern ALLOWED_STMT = java.util.regex.Pattern.compile(
    "^(INSERT|DELETE|--)",
    java.util.regex.Pattern.CASE_INSENSITIVE);

// In executeSQLFile, replace the execute call with:
String sql = stmt.toString().trim();
if (!ALLOWED_STMT.matcher(sql).find()) {
    // Skip DROP, CREATE USER, CALL, etc.
    stmt = new StringBuilder();
    continue;
}
con.createStatement().execute(sql);
```

The backup file only ever needs `DELETE` and `INSERT` statements (as generated by `backupToSQL`). Blocking everything else eliminates the attack surface.

**Defense-in-depth:** Sign or hash backup files so tampering is detectable. Simple approach: append SHA-256 of content as a `-- SHA256: <hash>` comment, verify on restore.

---

### SEC-4 — Image path read without sanitization (limited path traversal)
**Severity: Low** | CWE-22

**Evidence:**

```java
// DetallesLlibrePanelControl.java:112-114 (previewPortada)
private void previewPortada() {
    String path = this.vista.getTextPortada().getText().trim();
    ...
    pendingBlob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));

// DetallesLlibrePanelControl.java:122-125 (carregarImatge)
private void carregarImatge(String path) {
    ...
    pendingBlob = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
```

Also:
```java
// MostrarBibliotecaControl.java:1152
return java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
```

`path` comes directly from the UI text field (user-typed string). There is no canonicalization, no basedir restriction, no extension check.

**Why it matters (reduced in desktop context):**  
The user already has full filesystem access. The specific concern here is:
1. The read data is stored as `imatge_blob` in the DB — any file up to available memory gets embedded in the database
2. If the DB is shared (MariaDB) or the backup file is shared, arbitrary file contents (SSH keys, config files) are silently embedded and transmitted

**Remediation:**

```java
// DetallesLlibrePanelControl.java — add validation helper:
private static final java.util.Set<String> ALLOWED_EXT =
    java.util.Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

private static boolean isAllowedImagePath(String path) {
    if (path == null || path.isBlank()) return false;
    String lower = path.toLowerCase();
    return ALLOWED_EXT.stream().anyMatch(lower::endsWith);
}

// In previewPortada / carregarImatge — guard before readAllBytes:
if (!isAllowedImagePath(path)) {
    vista.getLabelIcono().setIcon(null);
    pendingBlob = null;
    return;
}
```

Add a size cap to prevent loading huge files into a BLOB:
```java
java.io.File f = new java.io.File(path);
if (f.length() > 10 * 1024 * 1024) return;  // 10 MB max
pendingBlob = java.nio.file.Files.readAllBytes(f.toPath());
```

---

### SEC-5 — `lookupByISBN` URL parameter not encoded
**Severity: Low** | CWE-20

**Evidence:**

```java
// OpenLibraryClient.java:19
String json = fetch("https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn
    + "&format=json&jscmd=data");
```

`lookupByTitle` and `lookupByAutor` correctly use `URLEncoder.encode()` (lines 41, 61). `lookupByISBN` does not.

**Why it matters:**  
An ISBN typed as `9780306406157&jscmd=viewapi` would modify the actual HTTP request sent to OpenLibrary. The URL is hardcoded to `openlibrary.org` so there is no SSRF risk to internal services. The realistic impact is a garbled API request returning unexpected data and potentially confusing the regex parser.

**Remediation:**

```java
// OpenLibraryClient.java:19 — replace:
String json = fetch("https://openlibrary.org/api/books?bibkeys=ISBN:"
    + java.net.URLEncoder.encode(isbn, StandardCharsets.UTF_8)
    + "&format=json&jscmd=data");
```

---

### SEC-6 — Missing field length validation in `LlibreValidator`
**Severity: Low** | CWE-20

**Evidence:**

```java
// herramienta/LlibreValidator.java:12-37
// Validates: ISBN digit count, nom not blank, valoracio 0-10, preu >= 0
// Does NOT validate: max string length for any field
```

DB schema (ServerConect.java:22-33):
```
nom        VARCHAR(255)
autor      VARCHAR(255)
descripcio VARCHAR(512)
notes      VARCHAR(2048)
editorial  VARCHAR(255)
serie      VARCHAR(255)
imatge     VARCHAR(255)
```

**Why it matters:**  
- H2 in MySQL mode silently truncates strings to column length. Data loss without error.
- MariaDB in strict mode throws `Data too long for column`. User sees an opaque error.
- `notes` is 2048 chars; if a user pastes a very long text from clipboard it silently truncates in H2.

**Remediation:**

```java
// LlibreValidator.java — add to checkLlibre():
if (nom.length() > 255)
    throw new IllegalArgumentException("El nom no pot superar 255 caràcters");
if (autor != null && autor.length() > 255)
    throw new IllegalArgumentException("L'autor no pot superar 255 caràcters");
if (descripcio != null && descripcio.length() > 512)
    throw new IllegalArgumentException("La descripció no pot superar 512 caràcters");
// notes validated in DetallesLlibrePanelControl before calling checkLlibre
```

Or centralise limits:
```java
// LlibreValidator.java
private static final int MAX_NOM  = 255;
private static final int MAX_DESC = 512;
private static final int MAX_NOTE = 2048;

private static String cap(String s, int max, String field) {
    if (s != null && s.length() > max)
        throw new IllegalArgumentException(field + " supera " + max + " caràcters");
    return s;
}
```

---

### SEC-7 — Date fields accepted without format validation
**Severity: Low** | CWE-20

**Evidence:**

```java
// Llibre.java:139
public void setDataCompra(String d) { DataCompra = (d != null && !d.trim().isEmpty()) ? d.trim() : null; }

// ServerConect.java:307
if (dc != null) {
    try { ps.setDate(17, java.sql.Date.valueOf(dc)); }
    catch (IllegalArgumentException e) { ps.setNull(17, java.sql.Types.DATE); }  // ← silent loss
}
```

`java.sql.Date.valueOf` requires strict `yyyy-MM-dd` format. Any other format (e.g. `15/01/2024`, `Jan 15`) silently becomes NULL in the DB.

**Remediation:**

```java
// LlibreValidator.java or Llibre.setDataCompra — add:
private static final java.util.regex.Pattern DATE_PATTERN =
    java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

public void setDataCompra(String d) {
    if (d == null || d.trim().isEmpty()) { DataCompra = null; return; }
    if (!DATE_PATTERN.matcher(d.trim()).matches())
        throw new IllegalArgumentException("Data invàlida: usa el format yyyy-MM-dd (era: " + d + ")");
    DataCompra = d.trim();
}
```

Alternatively, add a date-picker widget in the UI and remove free-text entry.

---

## Validation Matrix — User Input Surfaces

| Input Surface | Location | SQL-safe? | Length validated? | Type validated? | Notes |
|---|---|---|---|---|---|
| Book ISBN | `LlibreValidator:15-17` | ✓ (PreparedStmt) | — (numeric) | ✓ (digit count) | No Luhn checksum |
| Book nom | `LlibreValidator:19` | ⚠ (backupToSQL string format) | ✗ | ✓ (not blank) | See SEC-1 |
| Book autor | `afegirLlibre:292` | ✓ (PreparedStmt) | ✗ | — | sync also via PreparedStmt |
| Book descripcio | `afegirLlibre:294` | ✓ (PreparedStmt) | ✗ | — | |
| Book valoracio | `LlibreValidator:22-23` | ✓ | — | ✓ (0–10) | |
| Book preu | `LlibreValidator:25-26` | ✓ | — | ✓ (≥ 0) | |
| Book notes | `afegirLlibre:300` | ✓ (PreparedStmt) | ✗ | — | 2048 char DB limit |
| Book pagines | `Llibre:129` | ✓ | — | ✓ (clamped ≥ 0) | |
| Book editorial/serie | `afegirLlibre:302-305` | ✓ | ✗ | — | |
| Book dataCompra | `ServerConect:307` | ✓ | — | ✗ (silent null on bad format) | SEC-7 |
| Book idioma/format | `ServerConect:311-314` | ✓ | ✗ | — | |
| Image file path | `DetallesLlibrePanelControl:113` | ✓ | ✗ | ✗ | SEC-4 |
| Search bar | `aplicarSearchBar:592` | N/A (client-side filter) | — | — | RowFilter string match |
| Filter fields (nom/autor/etc.) | `filtrar:1024` | ✓ (PreparedStmt via domain) | — | ✓ (numeric: try/catch) | |
| ISBN scan input | `escanejarISBN:731` | ✓ | — | — | lookupByISBN URL not encoded (SEC-5) |
| CSV import | `importarCSV:676` | ✓ (PreparedStmt) | ✗ | ✓ (try/catch per field) | Malicious CSV → SEC-1 via backup |
| SQL restore file | `executeSQLFile:406` | ✗ | N/A | ✗ | SEC-3 |
| Shelf nom | `createLlista:482` | ✓ (PreparedStmt) | ✗ | — | |
| Tag nom | `createTag:660` | ✓ (PreparedStmt) | ✗ | — | UNIQUE constraint enforced |
| Loan person name | `addPrestec:610` | ✓ (PreparedStmt) | ✗ | — | |
| DB password (config) | `ServerConect:103` | N/A | — | — | SEC-2: in URL string |
| DB host | `ServerConect:103` | — | — | ✗ | No hostname format validation |

**Legend:** ✓ = handled  ✗ = not handled  ⚠ = partial  — = N/A or not applicable

---

## Prioritized Fix List

```
Priority 1 (SEC-1):  Fix sqlEsc to escape backslash
  File:   src/domini/ControladorDomini.java:255
  Change: s.replace("\\", "\\\\").replace("'", "''")
  Test:   Add apostrophe+backslash round-trip test to BibliotecaTest

Priority 2 (SEC-2):  Move MariaDB creds to Properties
  File:   src/persistencia/ServerConect.java:102-106
  Change: Pass user+pw to connectViaDriver instead of embedding in URL
  Test:   Unable to verify without MariaDB instance

Priority 3 (SEC-3):  Whitelist statement types in executeSQLFile
  File:   src/persistencia/ServerConect.java:406
  Change: Reject statements not matching ^(INSERT|DELETE)
  Test:   Add test: restore file with DROP TABLE → table survives

Priority 4 (SEC-4):  Validate image file extension + size cap
  File:   src/presentacio/detalles/control/DetallesLlibrePanelControl.java:112-125
  Change: Check extension in ALLOWED_EXT set; cap at 10 MB
  Test:   Manual verify: type /etc/passwd as portada path → rejected

Priority 5 (SEC-5):  URLEncode ISBN in lookupByISBN
  File:   src/herramienta/OpenLibraryClient.java:19
  Change: Wrap isbn with URLEncoder.encode(..., UTF_8)
  Test:   Unit test with isbn = "x&jscmd=evil" → URL not modified
```
