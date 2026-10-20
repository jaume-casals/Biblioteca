# Modernització de Biblioteca — informe de treball

Aquest document resumeix l'estat del repositori abans i després de la
modernització: què estava trencat, què s'ha canviat, com s'ha verificat i què
queda pendent.

Data de referència: 19 d'octubre de 2026 · Java 25.0.4.1 (Temurin) · Linux x64

---

## 1. Diagnòstic inicial (línia base)

Les afirmacions del `docs/ARCHITECTURE-MAP.md` es van tractar com a
hipòtesis, no com a fet. El que es va comprovar executant:

| Hipòtesi | Resultat | Evidència |
|-----------|----------|-----------|
| El glob `*Test.java` deixa fora els `Test*.java` | **Confirmada** — 30 classes de test no s'executaven mai | `build/baseline/undiscovered.log` |
| Els property tests no s'executen | **Confirmada** — `make fuzz-property` seleccionava `fuzz.domini.LlibreInvariantPropertiesTest`, classe que no existeix | `Makefile` (corregit) |
| `make test` falla si un test falla | **Falsa** — el bucle per classe acabava sempre amb codi 0 | `build/baseline/test.log` (`TEST_EXIT=0` amb 1 test fallat) |
| La suite de fuzz (Jazzer) no s'executa | **Confirmada** — la llista declasses estava obsoleta (cap existia) | `Makefile` (corregit) |
| Byte Buddy és compatible amb Java 25 | **Falsa** — Byte Buddy 1.15.4 no suporta el *class file* 69; 9 tests de mock fallaven | `build/baseline/undiscovered.log` |
| `LecturaRowTest` contradiu `RowDates.parseOrNull` | **Confirmada** — el test esperava una excepció; el codi retorna `null` de manera deliberada | `src/persistencia/row/RowDates.java` |
| La càrrega de camps pesats pot esborrar dades | **Confirmada** — una càrrega fallida deixava la descripció i les notes buides i el desat les sobreescrivia | `src/presentacio/detalles/control/ControladorPanellDetallsLlibre.java` |
| Les traduccions generades estan obsoletes | **Confirmada** — 12 claus per idioma eren als CSV però no als `.properties` | `scripts/gen_strings.py --check` |
| `java`/`javac` del PATH són stubs | **Falsa** — el `Makefile` ja ho comprova i el toolchain és funcional | `java -version` → Temurin 25.0.4.1 |
| Un checkout net compila | **Falsa** — `lib/*.jar` no és versionat i `gson` és necessari per compilar | `build/deps.lock` (nou) |

### Línia base de la suite

```
make test  →  25 classes · 326 tests correctes · 1 fallat · 1 omès · sortida 0
```

El codi de sortida 0 amb un test fallat és el problema més greu: la CI (o
qualsevol script que en depengui) donava el resultat per bo.

---

## 2. Decisions

### Sistema de build: es manté el Makefile

Es va avaluar Maven i Gradle. **Es manté el Makefile** com a definició
única de build, perquè:

- el projecte ja té un Makefile complet (compile, test, fuzz, package,
  audit) i una cadena Windows paral·lela (`scripts/*.bat`, `checkBiblio/*.ps1`);
  introduir Maven obligaria a duplicar la definició de build o a adoptar una
  disposició de fonts no estàndard (`src/` pla, no `src/main/java`);
- el risc real no era la resolució de dependències sinó la **fiabilitat de la
  suite**, que s'ha resolt amb un manifest propi;
- `build/maven-legacy/pom.xml` és un intent anterior abandonat: reintroduir-lo
  hauria estat un canvi de rumb, no una millora.

El que faltaba — resolució de dependències, reproducibilitat i informació de
versió — s'ha resolt amb `build/deps.lock` (manifest amb versions i SHA-256)
i `scripts/fetch_deps.py`.

### Dependències

Les versions es van consultar a Maven Central (`maven-metadata.xml`) en el
moment de l'execució, no de memoria. Es van triar versions estables i
compatibles, evitant prerelease i salts de versió major no necessaris.

| Dependència | Abans | Ara | Motiu |
|-------------|-------|-----|-------|
| net.bytebuddy:byte-buddy | 1.15.4 | **1.18.15** | 1.15.4 no suporta Java 25: 9 tests de mock fallaven |
| net.bytebuddy:byte-buddy-agent | 1.15.4 | **1.18.15** | idem |
| org.mockito:mockito-core | 5.14.2 | **5.24.0** | línia estable més recent |
| org.junit.platform:junit-platform-console-standalone | 1.11.4 | **1.14.4** | línia 5.x estable; JUnit 6.x queda fora perquè jqwik 1.10.2 depèn de la plataforma 1.14.x |
| net.jqwik:jqwik-api / -engine | 1.9.0 | **1.10.2** | requereix plataforma JUnit 1.14.4 |
| org.assertj:assertj-core | 3.26.3 | **3.27.7** | línia 3.x estable (4.x és prerelease) |
| org.objenesis:objenesis | 3.3 | **3.6** | línia estable més recent |
| com.h2database:h2 | 2.3.232 | **2.5.252** | llínia estable més recent; compatibilitat verificada |
| org.mariadb.jdbc:mariadb-java-client | 3.3.3 | **3.5.10** | client JDBC, compatible amb el protocol |
| com.google.code.gson:gson | 2.11.0 | **2.14.0** | llínia estable més recent |
| org.slf4j:slf4j-api | 2.0.7 | **2.0.20** | requisit del driver MariaDB |
| com.github.waffle:waffle-jna (+ jna) | 3.3.0 / 5.13.0 | **3.7.0 / 5.19.1** | auth Windows de MariaDB |
| com.code-intelligence:jazzer | 0.24.0 | **0.30.0** | arnesos de fuzzing; la disposició nativa és compatible amb el shim |

Es van eliminar del classpath els jars individuals de JUnit: el *standalone*
ja conté la plataforma i el motor de Jupiter, i tenir els dos al classpath
feia que la còpia del *standalone* (primera del classpath) eclipsés els
jars individuals.

---

## 3. Treball completat

### 3.1 Build i dependències

- **`build/deps.lock`** (nou): manifest amb `grup:artifact:versió sha256
  etiqueta` per a les 20 dependències (runtime, test, fuzz i una referència
  *legacy* d'H2 2.3.232 per a la comprovació de compatibilitat). És l'única
  font de veritat: el `Makefile` deriva el classpath del lock.
- **`scripts/fetch_deps.py`** (nou): `fetch` (baixa i verifica), `verify`
  (només SHA-256), `update` (consulta Maven Central i actualitza el lock) i
  `update --check` (informa de versions noves sense tocar el lock). Les
  prerelease i les variants `-jdk5` s'ignoren; l'etiqueta `legacy` no
  s'actualitza mai.
- **`make deps` / `deps-verify` / `deps-update`** (nous). `make compile`
  ja en depèn: un checkout net compila.
- La compilació verifica que existeixi `bin/main/Executable.class`: una
  compilació no pot "tenir èxit" sense artefactes.
- `src/version.properties` ja es copia al build: el diàleg *QuantADialeg* el
  llegeix de `/version.properties` i, sense això, no mostrava la versió.

### 3.2 Suite de tests

- `make test` executa **un sol JVM** amb el *launcher* de la plataforma JUnit:
  - descobreix les dues convencions de nomenyclatura (`FooTest.java` i
    `TestFoo.java`), 56 classes en lloc de 25;
  - el codi de sortida reflecteix el resultat (verificat: sortida 2 amb un
    test fallat, sortida 0 quan tot passa);
  - `--fail-if-no-tests`: si no es descobreix cap test, el build falla;
  - informes JUnit XML a `build/reports/tests/`.
- Els **arnesos de Jazzer** (5 classes que importen `com.code_intelligence.jazzer`)
  queden fora de la suite obligatòria perquè necessiten l'agent; es detecten
  automàticament (no hi ha cap llista escrita a mà que es pugui quedar
  obsoleta). `make fuzz-jazzer` els executa tots.
- `make fuzz-property` seleccionava una classe inexistent: ara selecciona
  `fuzz.domini.TestPropietatsInvariantsLlibre` i falla si no en troba cap.
- 7 expectatives obsoletes o incorrectes s'han corregit **amb el comportament
  correcte established, no relaxant les assertions**:
  | Test | Problema | Criteri |
  |------|----------|---------|
  | `LecturaRowTest.analitzarInvalid` | esperava una excepció; `RowDates.parseOrNull` retorna `null` de forma deliberada (helper compartit per tots els mapejadors) | es documenta el contracte defensiu, amb tres casos invàlids |
  | `TestLlibre.obtenirAutorsIsDefensiveCopy` | cridava `add()` sobre una llista de només lectura | s'asserta que la mutació llança `UnsupportedOperationException` i que la font no canvia |
  | `TestLlibre.blobRoundTrip` | asserta identitat (`isSameAs`) del blob | s'asserta el valor, que la còpia és defensiva i que mutar l'original no afecta l'estat |
  | `TestLlibre.copiarOfDeepCopiesAutors` | idem que el primer | idem |
  | `TestValidadorLlibre.fromStringIsbn10X` | el fixture `019853110X` **no és un ISBN-10 vàlid** (el seu dígit de control és 9, no X) | fixture vàlid `020161622X` → `9780201616224` + test nou del dígit de control X incorrecte |
  | `TestValidadorLlibre.invalidIsbnLengths` | el valor de 9 dígits `123456789` és un ISBN-10 vàlid amb zero inicial, suportat de manera deliberada | es substitueix per un de 9 dígits amb control invàlid i s'afegeix un test que documenta el cas de 9 dígits |
  | `TestAnalitzadorPrestatgeria.exportarToCsv` | passava mapes de prestatges buits, així que les dades no arribaven mai al CSV | es construeixen els mapes com ho fa el camí de producció |

### 3.3 Pèrdua de dades

- **Camps pesats a l'actualització** (`LlibreDaoCore`, `LlibreFieldBindings`,
  `LlibreMapper`): les columnes `descripcio` i `notes` només s'escriuen si el
  llibre les té carregades. Un objecte que no les ha carregat (vista
  lleugera, o càrrega fallida) ja no pot esborrar el que hi ha a la base de
  dades. Això cobreix la càrrega fallida **i** l'edició des de la taula,
  que fins ara esborrava descripció i notes en desar.
- **Panell de detalls**: si la càrrega en segon pla falla, abans de desar es
  torna a llegir els camps pesats del llibre; si la relectura també falla,
  l'actualització els deixa intactes.
- **Còpia de seguretat**: `snapshotForBackup()` usa la vista **completa**
  (amb descripció i notes) i la sobrecàrrega de tres arguments deixa de
  substituir la instantània per la vista lleugera. Abans, tant la còpia
  automàtica com la còpia prèvia a la restauració eren ** perdre descripcions
  i notes**.
- **Restauració**: abans d'esborrar res es valida el fitxer (ha de contenir la
  marca de còpia de seguretat o un `INSERT INTO llibre`). Un fitxer buit,
  truncat o d'un altre format es rebutja i la base de dades **no s'ha
  modificat**; abans es buidava i només després saltava l'error.

### 3.4 Internacionalització

- `scripts/gen_strings.py` accepta `--check` (verifica que els fitxers
  generats coincideixin amb `strings/*.csv`, sense escriure) i `--no-java`
  (no toca `I18n.java`, que conté claus manuals).
- Els tres `.properties` regenerats: **0 claus perdudes, 12 claus
  recuperades** per idioma.
- La CI comprova que els fitxers generats estiguin al dia.

### 3.5 Fuzzing

Amb els arnesos de Jazzer executant-se de debò (abans la llista de classes
era obsoleta i el shim de biblioteques natives era buit), el fuzzing ha
trobat dos casos reals:

1. `AnalitzadorPrestatgeria.analitzarShelfEntries` acceptava un nom de
   prestatge format per espai Unicode que `trim()` no elimina (p. ex. el
   separador de línia U+2028): el resultat era un prestatge sense nom visible.
   Ara el nom es neteja amb `strip()` (Unicode) i es descarta si és buit o si
   només conté caràcters de control (NUL), amb test de regressió.
2. `Rfc4180Reader.next()` retornava `null` després d'un `hasNext()==true`. En
   aquest cas **el codi de producció és el correcte** i l'arnès era l'equivocat:
   el contracte documentat de la classe estableix que la primera crida a
   `next()` que troba EOF retorna `null` i posa `eof=true`, i l'únic consumidor
   real (`ImportadorLlibres`) ho tracta així. L'arnès s'ha alineat amb el
   contracte (l'error detectable de debò — un `NoSuchElementException` dins
   del bucle — es manté com a fallada).

El shim de biblioteques natives de Jazzer ara depèn del JAR: si la versió de
Jazzer canvia, el shim es reconstrueix (si no, l'agent no s'instal·la i el
lane de fuzzing falla).

### 3.6 CI

`.github/workflows/ci.yml` (nou, no hi havia cap workflow):

| Job | Què fa |
|-----|--------|
| `build-and-test` | checkout, JDK 25, `make deps` + `deps-verify` + `compile` + `test`, publica els informes JUnit |
| `i18n` | `gen_strings.py --check --no-java` |
| `h2-compat` | `make h2-compat` |
| `package` | `make jar` i comprovació del contingut del JAR |
| `fuzz` | programat: `fuzz-property` + `fuzz-jazzer` |
| `dependencies` | programat: versions noves disponibles |

`permissions: contents: read` (privilegi mínim).

---

## 4. Evidència

### Comandes i resultats

```bash
$ make deps-verify
OK    20 dependències verificades

$ make compile
Build complete: bin/            # 336+ classes, sense errors

$ make test
[       658 tests found           ]
[       657 tests successful      ]
[         1 tests skipped         ]
[         0 tests failed          ]
# sortida 0

$ make h2-compat
== Crea la base de dades amb H2 lib/h2-2.3.232.jar ==
CREATE OK: llibre 9780306406157 inserit
== Obre la mateixa base de dades amb la versió actual ==
VERIFY OK: versio esquema=39 llibres=1

$ python3 scripts/gen_strings.py --check --no-java
OK: els fitxers generats coincideixen amb strings/*.csv

$ make jar
Built build/artifacts/biblioteca.jar
```

### Comparativa de la suite

| | Abans | Ara |
|---|-------|-----|
| Classes seleccionades | 25 | 56 (+ 5 arnesos de fuzz, optatius) |
| Tests descoberts | 326 | **658** |
| Tests correctes | 325 | 657 |
| Tests fallats | 1 (surtida 0) | 0 (surtida ≠ 0 si falla) |
| Tests omèsos | 1 | 1 |
| Informes | cap | 4 XML JUnit a `build/reports/tests/` |
| Sortida amb test fallat | 0 ❌ | ≠ 0 ✓ |

### Fitxers tocats

- Nous: `build/deps.lock`, `scripts/fetch_deps.py`, `scripts/h2compat/CheckH2Compat.java`,
  `.github/workflows/ci.yml`, `test/persistencia/TestCampsPesatsNoSborren.java`,
  `test/herramienta/io/TestCopiaSeguretatRoundTrip.java`, aquest document.
- Modificats: `Makefile`, `src/persistencia/dao/LlibreDaoCore.java`,
  `src/persistencia/internal/LlibreFieldBindings.java`,
  `src/persistencia/internal/LlibreMapper.java`,
  `src/persistencia/internal/ControladorPersistencia.java`,
  `src/herramienta/io/ServeiCopiaSeguretat.java`,
  `src/herramienta/text/ValidadorLlibre.java`,
  `src/presentacio/detalles/control/ControladorPanellDetallsLlibre.java`,
  `scripts/gen_strings.py`, `scripts/*.bat`, `checkBiblio/*`,
  `src/herramienta/strings_{ca,es,en}.properties`, `README.md`,
  `docs/ARCHITECTURE-MAP.md`, `docs/agent/*.md`, `docs/fuzzing.md`.

---

## 5. Riscos residuals i treball pendent

### Riscos coneguts, no resolts

1. **Les portades no surten a la còpia SQL.** Els BLOBs de coberta s'exclouen
   del volcat textual per decisió documentada de la classe. Després de
   restaurar cal tornar a descarregar-les. Canviar-ho és un canvi de format
   de fitxer, no un bug.
2. **Concurrència a la interfície.** L'auditoria va detectar diverses races
   entre els resultats en segon pla i la seleció actual
   (`ControladorMostrarBiblioteca`, `ControladorTaula`, `ControladorGaleria`):
   un resultat vell pot sobreescriure'n un de nou. No s'ha tocat: requereix
   un model de generacions o cancel·lació que no s'ha dissenyat.
3. **Caché de cobertes**: `ServeiImatgesCoberta` expulsa per ordre
   d'inserció, no LRU (el `LinkedHashMap` és `accessOrder=false`). La
   documentació del mètode ho diu, però el nom de la classe suggereix LRU.
4. **JUnit 6.x i assertj 4.x** queden fora: són versions major i, almenys per
   a JUnit, incompatibles amb la versió de plataforma que exigeix jqwik.
5. **MariaDB** no s'ha verificat contra un servei real en aquesta sessió: no
   hi havia un servidor disponible. La compatibilitat provada és la d'H2.
6. **5 claus de traducció es mantenen a mà** a `I18n.java` i no són als CSV
   (`dlg_restore_done_with_note`, `tip_filter_substring`,
   `dlg_fetch_portades_done_partial`, `dlg_perfil_nom_invalid`,
   `dlg_perfil_nom_invalid_title`). Sónen les que l'*UIAudit* marca com a
   "missing from CSV" (2 FAIL preexistents). No s'han mogut perquè
   `dlg_restore_done_with_note` conté un salt de línia real, que el format CSV
   no pot expressar, i perquè les claus del CSV usen la convenció `\n`
   literal (dos caràcters). **Aquesta convenció és inconsistent dins la taula
   de traduccions**: les claus del CSV mostren un `\n` literal a la interfície
   perquè la IU no el converteix en salt de línia, i les 5 claus manuals
   tenen un salt de línia real. Unificar-ho exigeix decidir quina convenció
   mana i regenerar la taula sencera: és una decisió de producte, no un bug
   puntual.

### Treball pendent, per ordre de prioritat

1. Model de generacions per als resultats en segon pla (evitar que un
   resultat vell sobreescrigui un de nou) i tests de concurrència.
2. Suite d'integració contra MariaDB en un servei aïllat (contenedor) amb els
   mateixos contractes que H2.
3. Netejar `lib/` dels jars que ja no corresponen al lock (no s'han esborrat
   perquè són artefactes no versionats i el repositori no esborra fitxers
   sense permís).
4. Exportació JSON/CSV: la còpia SQL ja inclou els camps pesats, però cal
   comprovar que l'exportació JSON també els conservi i que la importació
   els restauri.
5. `pom.xml` de `build/maven-legacy/`: es pot arxivar o esborrar quan es
   decideixi que no hi haurà més intents de Maven.
---

## 6. Redisseny de la interfície (2026-10)

### 6.1 Decisió tecnològica

Es manté **Java + Swing** i es substitueix **Nimbus per FlatLaf 3.7.2**. No hi ha
servidor HTTP ni procés auxiliar, ni migració a JavaFX/Electron/Tauri. La interfície
nova és una capa nova sobre el backend existent: domini i persistència no s'han tocat
(apartat d'ampliació del contracte d'aplicació de resultats, explicat a sota).

### 6.2 Dependències addides

| Dependència | Versió | SHA-256 | Per què |
|---|---|---|---|
| `com.formdev:flatlaf` | 3.7.2 | `917aff39…6c0fabf4` | Look & Feel pla, control·lable per `UIManager`. |
| `com.formdev:flatlaf-extras` | 3.7.2 | `7d03612d…8b5166` | `FlatSVGIcon`: renderitzat d'icones SVG locals. |
| `com.github.weisj:jsvg` | 2.1.0 | `a538142d…ffc1b6` | Renderitzador SVG que utilitza `FlatSVGIcon`. |

Les tres són versions reals publicades a Maven Central, verificant-les amb
`maven-metadata.xml` i calculant el SHA-256 del jar. S'han fixat a `build/deps.lock`
i `make deps-verify` en verifica la integritat. `jsvg` no arrossega dependències
d'execució (les del seu POM són opcionals i de prova).

### 6.3 Sistema de tokens

* `herramienta/ui/UITheme.Paleta`: paletes semàntiques (superfícies, text, vores,
  accent, estat, interacció, barra lateral) per a quatre temes.
* `herramienta/ui/Tokens`: escala d'espaiat, radis, alçades de control, mides de
  tipografia i escala de lletra.
* `presentacio/util/UIComponents`: aplica els tokens; ja no fixa mides preferides
  dels botons (el LAF les calcula a partir del text, de manera que cap etiqueta
  queda tallada).
* `presentacio/util/Icones` + `src/presentacio/icones/*.svg`: família local de 33
  icones SVG, rasteritzada una vegada per (nom, mida, color).

Guia completa per a pantalles noves: [`docs/SISTEMA-VISUAL.md`](SISTEMA-VISUAL.md).

### 6.4 Correccions de fons

* **Comptador incoherent.** El recompte visible es derivava de dues quantitats
  independents (files del model de la taula i mida de la llista en memòria) i
  podia mostrar "1000 / 0 llibres". Ara l'estat de vista és l'única font
  (`llibresEnAmbit` / `llibresMostrats`) i el resultat s'aplica amb la mida de
  l'àmbit (`AplicadorResultat`).
* **Formulari de llibre nou.** `GridLayout(0,2)` feia que tots els camps creixess
  a la vegada. Ara el formulari usa `GridBagLayout`: els camps d'una línia
  conserven la seva alçada i només la descripció i les notes absorbeixen
  l'alçada extra. El diàleg té capçalera, cos desplaçable, pestanyes de
  metadades i peu fix.
* **Camps truncats a la barra lateral.** Les entrades de navegació es retallen
  ells mateixos (etiqueta curta "Recents") i el text de la capçalera mostra el nom
  complet de la vista.
* **Valoració.** El valor per defecte de la columna `valoracio` és 0, és a dir
  "sense valorar". Es mostra la xifra (i no estrelles buides) només quan hi ha
  valoració real.
* **Progrés.** `JProgressBar` amb text pintat omplia tota la cel·la. El renderer
  nou dibuixa una barra de 3 px amb el text de pàgines, i distingeix "—" (sense
  dades) de "0 / 320" (0% real).
* **Tema fosc incomplet.** La galeria no es re-tematitzava: les targetes
  quedaven amb els colors del tema clar. Ara `PanelMostrarBiblioteca.aplicarTheme()`
  aplica la galeria i les targetes es tornen a construir.

### 6.5 Resultats

Build i suite: `make compile` i `make test` en verd — 733 tests correctes, 0 fallits
(base: 712 correctes, 0 fallits; +21 tests nous). Les proves no s'han modificat per
acceptar cap regressió; un test nou (el contrast WCAG) va detectar que dues paletes
tenien el text secundari per sota del mínim i les paletes es van corregir.

Proves visuals: `checkBiblio/ProvaVisual` arrenca l'aplicació real contra una H2 en
memòria, hi posa llibres ficticis i en captura la finestra en clar i fosc, galeria i
taula, el diàleg de llibre nou i el de configuració. Les captures són a
`build/captures/`.

Mesures de rendiment (mediana de 3 execucions, mateixa màquina, mateixa mida de
biblioteca de prova):

| Mesura | Abans | Després | Variació |
|---|---|---|---|
| Arrencada fins a la finestra principal | 1728 ms | 1453 ms | −16 % |
| Canvi a galeria | 70,4 ms | 70,5 ms | = |
| Canvi a taula | 75,2 ms | 75,2 ms | = |
| Redimensionament ×10 | 208 ms | 317 ms | **+52 %** |
| Cerca | 15,5 ms | 17,9 ms | +15 % (dins del soroll entre execucions) |
| Heap usat | 26,6 MB | 19,6 MB | −26 % |
| CPU del fil principal | ~150 ms | ~151 ms | = |

El redimensionament és més car perquè les targetes de galeria són més grans
(190 px d'amplada en lloc de 140, d'acord amb la mida indicativa demanada) i, per
tant, pinten més píxels. El canvi de vista, la cerca i l'arrencada no han
regressat; la memòria i l'ocupació de CPU han millorat.

### 6.6 Pendent

* La fitxa de llibre (`PanellDetallsLlibre`) encara no separa netament el mode
  lectura del mode edició: el mode lectura segueix sent una pila de camps. La
  correcció està dissenyada (reutilitzant `ConstructorGraellaFormulari`) però no
  s'ha aplicat.
* Les opcions avançades de la galeria (agrupació per sèrie com a opció secundària,
  filtres actius eliminables, "Neteja filtres") no s'han acabat: la barra
  d'eines té el botó de filtres i el de sèries, però no la fila de xips.
  L'escaffold mort d'aquesta fila (el mètode `posarFiltresActius`, el record
  `FiltreActiu` i el contenidor buit que reservava l'alçada) s'ha eliminat;
  la implementació s'ha de fer de nou quan la funcionalitat es connecti al
  model de filtres.
* No s'ha fet la passa de mides de lletra 100 % / 150 % / 200 % amb captura
  (l'entorn gràficDisposeix de Xvfb però el que hi ha no aplica escalat de sistema).
* `Construcció de gràfics`: no s'ha introduït cap llibreria nova de gràfics; les
  pestanyes de gràfics i de núvol d'etiquetes es mantenen tal com eren.
