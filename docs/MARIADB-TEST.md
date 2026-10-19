# Contracte de persistència contra MariaDB

Aquest document explica com executar el **contracte de persistència** —les
mateixes afirmacions— contra un servidor **MariaDB real**, i no contra la
base de dades en memòria d'H2 que fa servir `make test`.

El contracte viu en `test/persistencia/`:

| Fitxer | Paper |
| --- | --- |
| `ContractPersistenciaBase.java` | Totes les afirmacions, agnòstiques del motor. Classe abstracta. |
| `ContractPersistenciaH2Test.java` | subclasse per a H2 en memòria. S'executa dins de `make test`. |
| `ContractPersistenciaMariaDBTest.java` | subclasse per a MariaDB. S'executa fora de `make test`, en una JVM separada. |

---

## 1. Requisits previs

* **Java 25** (el mateix JDK que fa servir `make compile`).
* **Un runtime de contenidors**: `podman` (recomanat) o `docker`. Si no n'hi ha
  cap, el contracte també es pot executar contra un servidor MariaDB existent
  (apartat 5).
* **Accés a la xarxa** la primera vegada, per baixar la imatge
  `docker.io/library/mariadb:11.4` (332 MB). Si la imatge ja és local, no cal
  xarxa.
* Les dependències del projecte: `make deps` (ja les baixa `build/deps.lock`).

Comprovar que el runtime hi és:

```bash
podman --version     # o: docker --version
```

---

## 2. Seqüència exacta de comandes

L'ordre és important. El pas 1 compila el codi i les classes de test; el pas 2
aixeca el servidor de rebut i executa el contracte; el pas 3 neteja.

```bash
# 1. Compilar (codi + classes de test)
make compile

# 2. Aixecar MariaDB de rebut i executar el contracte
make test-mariadb

# 3. (Opcional) Netejar contenidors de proves que hagin quedat en peu
make mariadb-down
```

`make test-mariadb` és **equivalent** a `scripts/mariadb_test.sh`; el script
imprime cada ordre exacta que executa.

El script, internament, fa:

```bash
# Contenidor de rebut: port d'amfitrió lliure assignat pel runtime
podman run -d --rm \
    --name biblioteca-mariadb-test-<pid>-<ts> \
    -e MARIADB_ROOT_PASSWORD=<contrasenya-temporal> \
    -e MARIADB_ROOT_HOST=% \
    -p 127.0.0.1::3306 \
    docker.io/library/mariadb:11.4

# Espera de disponibilitat (consulta real per TCP, no un ping que no valida res)
podman exec <contenidor> mariadb -h 127.0.0.1 -P 3306 --protocol=TCP \
    -uroot -p<contrasenya> -N -e "SELECT 1;"

# Contracte: JVM separada, sense biblioteca.h2.url
java -Dbiblioteca.test=true \
     -Dbiblioteca.mariadb.host=127.0.0.1:<port> \
     -Dbiblioteca.mariadb.user=root \
     -Dbiblioteca.mariadb.password=<contrasenya> \
     -Dbiblioteca.mariadb.database=BIBLIOTECA_TEST \
     -jar lib/junit-platform-console-standalone-<versió>.jar execute \
     --class-path bin:<classpath> \
     --select-class=persistencia.ContractPersistenciaMariaDBTest \
     --details=summary --disable-ansi-colors

# Esborrat del contenidor i del seu volum anònim (sempre, amb trap EXIT)
podman rm -f -v <contenidor>
```

### Per què una JVM separada

`make test` arrenca tota la suite en un sol JVM amb

```
-Dbiblioteca.h2.url="jdbc:h2:mem:biblioteca_suite;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
```

Aquesta propietat fa que `ControladorPersistencia` connecti amb **H2** en
lloc de MariaDB. Per tant, el contracte de MariaDB **no pot** viure dins de
`make test`: cal un llançament propi, sense aquesta propietat.

Dins de `make test`, la classe `ContractPersistenciaMariaDBTest` queda
**saltada** (no fallada) perquè no hi ha propietats de connexió: es queda
davant un `Assumptions.assumeTrue(...)` abans d'obrir cap connexió.

---

## 3. Aïllament: què no es toca mai

| Garantia | Com s'assegura |
| --- | --- |
| No es llegeix ni s'escriu `~/.biblioteca` | El contracte no passa per `Configuracio`: fa servir `ControladorPersistencia.create(cfg)`, que construeix la configuració de connexió a mà. La `Configuracio` (que registra un `shutdown hook` que escriu el fitxer de configuració) no s'inicialitza mai. |
| No s'escriu a l'esquema `BIBLIOTECA` de producció | L'esquema de proves és `BIBLIOTECA_TEST`. La classe de test rebutja explícitament `biblioteca.mariadb.database=BIBLIOTECA` amb un `assumeFalse`. |
| No es toquen contenidors, imatges, volums ni ports personals | El contenidor rep un nom amb el prefix `biblioteca-mariadb-test-` i s'esborra amb el seu volum anònim. El port d'amfitrió l'assigna el runtime (`-p 127.0.0.1::3306`), de manera que no se tria cap port concret. |
| La imatge pública no s'esborra | Només es fa `pull` si no és local. `mariadb-down` només esborra contenidors amb el prefix `biblioteca-mariadb-test-`. |
| `biblioteca.h2.url` no es posa en la execució de MariaDB | El script no passa aquesta propietat. Si existís, `ControladorPersistencia` connectaria amb H2 i el contracte no provaria res. |

---

## 4. Com apunta el contracte a un servidor extern

Per provar contra un MariaDB ja existent (per exemple un d'un CI o un de
paral·lel), sense contenidor:

```bash
MARIADB_TEST_HOST=db.example.org \
MARIADB_TEST_PORT=3306 \
MARIADB_TEST_USER=biblioteca \
MARIADB_TEST_PASSWORD='secret' \
MARIADB_TEST_DATABASE=BIBLIOTECA_TEST \
make test-mariadb
```

| Variable | Per defecte | Descripció |
| --- | --- | --- |
| `MARIADB_TEST_HOST` | (buit → contenidor) | Servidor extern. |
| `MARIADB_TEST_PORT` | `3306` | Port del servidor extern. |
| `MARIADB_TEST_USER` | `root` | Usuari. |
| `MARIADB_TEST_PASSWORD` | buit | Contrasenya. |
| `MARIADB_TEST_DATABASE` | `BIBLIOTECA_TEST` | Esquema on s'escriu. |
| `MARIADB_TEST_IMAGE` | `docker.io/library/mariadb:11.4` | Imatge del contenidor de rebut. |
| `KEEP_CONTAINER` | `0` | Si és `1`, el contenidor es deixa en peu al final. |

Si el servidor extern no permet crear esquemes, l'esquema l'ha de crear
prèviament:

```sql
CREATE DATABASE BIBLIOTECA_TEST CHARACTER SET utf8mb4;
```

### Deixar el servidor en peu per fer proves manuals

```bash
make mariadb-up     # aixeca el contenidor i NO l'esborra
make mariadb-down   # esborra els contenidors biblioteca-mariadb-test-*
```

---

## 5. Neteja

* Automàtica: el script esborra el contenidor i el volum anònim amb un
  `trap ... EXIT`, passi el que passi (també si el contracte falla).
* Manual: `make mariadb-down`.
* Amb `KEEP_CONTAINER=1` el contenidor es conserva; en aquest cas, el script
  imprimeix l'ordre exacta per aturar-lo.

---

## 6. Diferències reals entre els dos motors

El contracte **no amaga** les diferències: on H2 i MariaDB no es comporten
igual, hi ha una afirmació explícita que ho documenta. Aquestes són les que
s'han **mesurat** (no suposat) contra MariaDB 11.4.13 i H2 2.5.252:

### 6.1 Límit de mida de la coberta (`BLOB`)

MariaDB limita el tipus `BLOB` a **65535 bytes**; H2 no hi posa cap límit.

* Contracte: `limitDeMidaDeLaCoberta()` — a H2 una coberta de 100 KB s'hi
  desa; a MariaDB la mateixa coberta és rebutjada amb `SQLException` i una
  coberta de 65535 bytes sí que s'hi desa.
* Conseqüència per a l'aplicació: una portada de llibre de més de 64 KB no
  es pot desar a MariaDB. El camí d'H2 no ho detecta.

### 6.2 La cerca `LIKE` i la caixa

MariaDB usa per defecte una col·lació `*_ci` (case-insensitive); el `LIKE` d'H2
distingeix majúscules de minúscules.

* Contracte: `cercaSegonsLaCollacioDelMotor()` — la mateixa cerca retorna
  resultats diferents a cada motor, i l'afirmació ho declara explícitament.
* Conseqüència per a l'aplicació: la cerca per títol és més permissiva a
  MariaDB que a H2.

### 6.3 Tipus de retorn dels números

Una columna `FLOAT` retorna `java.lang.Float` a MariaDB i `java.lang.Double` a
H2. La taula `llibre` usa `DOUBLE` per a `valoracio` i `preu` (migracions 29
i 30), així que aquests dos camps sí que retornen `Double` als dos motors.
`llibre_llista.valoracio` també és `DOUBLE` (migració 31).

### 6.4 Operador `||`

A MySQL/MariaDB, `||` és **OR lògic** (`PIPES_AS_CONCAT` és desactivat per
defecte), no concatenació. El volcat de còpia de seguretat ja no l'utilitza:
utilitza `CONCAT(...)`, que significa el mateix als dos motors.

### 6.5 Escapada de la barra invertida

H2 **no** interpreta la barra invertida com a caràcter d'escapada dins les
literals de text; MySQL/MariaDB sí. Perquè el format del volcat sigui
portable:

* el volcat no duplica les barres invertides (SQL estàndard), i
* la connexió MariaDB afegeix `NO_BACKSLASH_ESCAPES` al `sql_mode` de la
  sessió (veure `ConnexioServidor.alinearSqlModeMariaDB()`).

Així una descripció amb `C:\rutes` es desa i es restaura **igual** als dos
motors. Abans, a H2 la barra es duplicava al restaurador.

> **Compatibilitat**: un volcat antic (generat abans d'aquest canvi, amb les
> barres invertides duplicades) es restaurará amb les barres duplicades a
> tots dos els motors. Només afecta a textos amb barres invertides.

### 6.6 `INSERT ... SELECT` en el protocol de sentències preparades

MariaDB rebutja (error 1295, *«This command is not supported in the prepared
statement protocol yet»*) un `INSERT ... SELECT` quan el lot té més d'una
fila. Per això la sincronització d'autors resol l'`id` amb un `SELECT` i
insereix amb un `INSERT ... VALUES`.

### 6.7 Declaracions que **no** han requerit canvis

Aquestes s'havien marcat com a sospitoses i s'han **verificat** amb un
servidor MariaDB real: funcionen igual que a H2, i no s'hi ha tocat res.

* `INSERT INTO llista (nom, ordre) VALUES (?, (SELECT COALESCE(MAX(ordre),0)+1 FROM llista AS sub))`
  — l'error 1093 es refereix a `UPDATE`/`DELETE`, no a l'`INSERT ... SELECT`
  d'aquest motor.
* La migració 33 (`INSERT IGNORE INTO llibre_autor ... NOT IN (SELECT isbn FROM llibre_autor)`).
* `CREATE INDEX IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`,
  `ALTER TABLE ... MODIFY COLUMN`, `GROUP_CONCAT(x ORDER BY y SEPARATOR ', ')`.

---

## 7. Què cobreix el contracte

* Esquema creat i **migracions aplicades** (`schema_version` 1..39) i creació
  idempotent.
* CRUD de llibres (crear, llegir, actualitzar, esborrar, ISBN duplicat).
* Relacions: autors (`llibre_autor`), etiquetes (`llibre_tag`) i llistes de
  lectura (`llibre_llista`), amb les files de la unió verificades a SQL.
* Filtres per nom, autor i any.
* Paginació: `obtenirLlibresPage` i `comptarLlibresDB`.
* Unicode: accents, CJK i emoji.
* Dates (inclòs un 29 de febrer) i valors nuls.
* Camps pesats: carregats, no carregats (un `UPDATE` no ha d'esborrar el que
  hi ha) i esborrats explícitament.
* Coberta `BLOB` comparada byte a byte, i el límit de mida segons el motor.
* Còpia de seguretat: còpia → esborrat → restauració, conservant autors,
  etiquetes, llistes, text multilínia, cometes simples i barres invertides.
  La coberta queda fora del volcat de manera deliberada i el contracte ho
  deixa escrit.
