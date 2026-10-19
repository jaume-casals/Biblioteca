<div align="center">

# Biblioteca

**Un gestor personal de biblioteca d'escriptori, fet amb Java i Swing.**

Catàleg · Prestatges · Etiquetes · Portades · Préstecs · Estadístiques · Mode fosc · Multi-idioma

[Característiques](#-característiques) ·
[Instal·lació ràpida](#-instal·lació-ràpida) ·
[Compilació](#-compilació) ·
[Ús](#-ús) ·
[Arquitectura](#-arquitectura) ·
[Tests](#-tests) ·
[Llicència](#-llicència)

</div>

---

## Característiques

### Catàleg i metadades

- **Llibres** amb títol, autors, ISBN-10/13, any, valoració (0-10), preu, portada i notes
- **Importació automàtica de cobertes** des d'OpenLibrary per ISBN
- **Emmagatzematge d'imatges** com a blobs dins la base de dades (sense fitxers externs)
- **ISBN-10 / ISBN-13** amb validació de dígit de control i conversió automàtica

### Organització

- **Prestatges il·limitats** — cada llibre pot tenir valoració i estat de lectura propis per prestatge
- **Etiquetes** per classificació lliure (gènere, autor, projecte, ...)
- **Cerca en temps real** per títol, autor, ISBN, descripció i notes
- **Filtres combinables** per valoració, any, idioma, format, prestatge, etiqueta i estat de lectura

### Productivitat

- **Importació / Exportació** — CSV, JSON, HTML i PDF
- **Còpia de seguretat SQL** amb restauració segura (l'aplicació fa còpia prèvia abans de restaurar)
- **Préstecs** — registra a qui has prestat cada llibre i segueix els retorns
- **Progrés de lectura** — pàgines llegides amb barra de progrés visible a la taula
- **Estadístiques** — mitjanes per prestatge, percentatges de lectura, autors més presents

### Interfície

- **Tres temes** — Clar, Fosc, Sepia i Ocean — commutable en calent
- **Mida de lletra** configurable (8 nivells)
- **Multi-idioma** — Català, Espanyol i Anglès
- **Dreceres de teclat** per a totes les accions principals

---

## Instal·lació ràpida

### Linux / macOS

```bash
# Descarrega o clona el repositori
git clone https://github.com/<usuari>/biblioteca.git
cd biblioteca

# Descarrega les dependències (verificades amb SHA-256) i compila
make deps
make compile
make run
```

### Windows

```bat
scripts\compile.bat
java -cp bin;lib\h2-2.5.252.jar;lib\mariadb-java-client-3.5.10.jar;lib\gson-2.14.0.jar main.Executable
```

> Les dependències es downloaden de Maven Central i es verifiquen amb el
> `SHA-256` de `build/deps.lock`; no cal `apt`, `brew` ni cap gestió de paquets externa.

### Instal·lador

També disponible un instal·lador gràfic per a Windows (`install.exe`) i un script de preparació per a Linux (`install.sh`).

---

## Compilació

| Objectiu | Comanda | Descripció |
|----------|---------|------------|
| Dependències | `make deps` | Baixa els jars de `build/deps.lock` i verifica el SHA-256 |
| Verificar deps | `make deps-verify` | Comprova el SHA-256 sense baixar res |
| Actualitzar deps | `make deps-update` | Consulta Maven Central i proposa versions noves |
| Compilar | `make compile` | `src/` → `bin/` (Java 25) |
| Tests | `make test` | Suite completa (JUnit 5 + jqwik) amb informes XML |
| Netejar | `make clean` | Elimina `bin/` |
| Executar | `make run` | Neteja + compila + obre la GUI |
| Empaquetar | `make jar` / `make fat-jar` | JAR fi amb manifest / JAR autònom |
| Compatibilitat H2 | `make h2-compat` | Obre una base de dades creada amb H2 2.3.232 |
| Fuzzing | `make fuzz-property` / `make fuzz-jazzer` | Property tests i coverage-guided fuzzing |
| Auditories | `./checkBiblio/run.sh` | Audit d'UI + tests d'estrès |

**Requisits:** JDK 25 o superior i Python 3 (per a `scripts/fetch_deps.py`).

### Dependències

`build/deps.lock` és l'única font de veritat de les versions: cada línia és
`grup:artifact:versió sha256 etiqueta` (`runtime`, `test`, `fuzz` o `legacy`).
El `Makefile` deriva el classpath del lock, de manera que les versions no es
poden desincronitzar entre el build i la documentació.

```bash
make deps          # baixa el que falti i verifica els SHA-256
make deps-verify   # només verificació (el que fa la CI)
make deps-update   # informa de versions noves a Maven Central
```

**Compilació manual** (sense Make):

```bash
find ./src/ -name "*.java" > classes.txt
javac --release 25 -cp lib/h2-2.5.252.jar:lib/mariadb-java-client-3.5.10.jar:lib/gson-2.14.0.jar @classes.txt -d bin
```

---

## Ús

### Dreceres de teclat

| Drecera | Acció |
|---------|-------|
| `Ctrl + N` | Nou llibre |
| `Ctrl + F` | Cerca ràpida |
| `Ctrl + E` | Edita el llibre seleccionat |
| `Ctrl + A` | Selecciona tots els llibres visibles |
| `Ctrl + Shift + D` | Canvia entre tema clar i fosc |
| `Supr` | Elimina els llibres seleccionats |
| `Intro` | Obre els detalls del llibre seleccionat |
| `Esc` | Tanca el quadre de cerca |

### Poblar amb dades d'exemple

Per provar l'aplicació amb milers de llibres d'OpenLibrary:

```bash
make populate          # ~2000 llibres
make populate MAX=500  # límit personalitzat
```

### Base de dades

Per defecte utilitza **H2 embeguda** — cap configuració necessària. La base de dades es desa a `~/.biblioteca/biblioteca`.

Per canviar a **MariaDB**, edita `~/.biblioteca/config.properties`:

```properties
dbType=mariadb
dbHost=localhost
dbUser=usuari
dbPassword=contrasenya
```

Les migracions d'esquema s'apliquen automàticament a l'inici; cada nova versió queda registrada a la taula `schema_version`.

---

## Arquitectura

Aplica el patró **MVC en tres capes** amb separació clara de responsabilitats:

```
┌─────────────────────────────────────────────────────────────┐
│  presentacio/   Swing — *Panel (disseny) + *Control (lògica) │
├─────────────────────────────────────────────────────────────┤
│  domini/        Lògica de negoci — ControladorDomini         │
│                 facade → StateContext (lock) → delegates     │
├─────────────────────────────────────────────────────────────┤
│  persistencia/  JDBC — ControladorPersistencia → DAO         │
│                 ConnexioServidor, migracions, H2/MariaDB         │
└─────────────────────────────────────────────────────────────┘
```

| Capa | Responsabilitat | Tecnologies |
|------|-----------------|-------------|
| `presentacio/` | Panells i controls Swing, listeners, renderers | Java 25, Swing, Nimbus L&F |
| `domini/` | Regles de negoci, validació, filtres, ordenació | Records, lambdes, `StateContext` amb `synchronized` |
| `persistencia/` | Connexió JDBC, DAOs, migracions, transaccions | H2 / MariaDB, `PreparedStatement` |
| `herramienta/` | Utilitats compartides (Config, UITheme, I18n, Validator) | `java.util.logging`, `ResourceBundle` |
| `interficie/` | Interfícies de callback per a esdeveniments de domini | — |

L'estat en memòria viu a `StateContext` i es serialitza a la base de dades en cada mutació — el patró és **write-through**. Tots els mètodes de `ControladorPersistencia` són `synchronized`, cosa que evita la doble sincronització als DAOs.

---

## Tests

La suite utilitza **JUnit 5**, **jqwik**, **AssertJ** i **Mockito**. Cada grup
de tests utilitza una base de dades H2 en memòria independent.

```bash
make test
```

`make test` executa la suite en un sol JVM amb el *launcher* de la plataforma
JUnit, de manera que:

- es descobreixen les dues convencions de nomenyclatura (`FooTest.java` i
  `TestFoo.java`);
- els errors de test fan fallar el build (codi de sortida ≠ 0);
- si no es descobreix cap test, el build falla igual (`--fail-if-no-tests`);
- els informes JUnit XML queden a `build/reports/tests/`.

| Target | Contingut | Execució |
|---------|-----------|----------|
| `make test` | Unitàries, integració H2, migracions, import/export, còpia de seguretat, Swing | Obligatòria (CI) |
| `make fuzz-property` | Property tests de jqwik (`fuzz/domini/TestPropietatsInvariantsLlibre`) | Optativa |
| `make fuzz-jazzer` | Arnesos coverage-guided de Jazzer | Optativa / programada |
| `make h2-compat` | Obertura d'una base de dades H2 antiga amb el driver actual | Programada |

**Estructura dels tests:**

| Tipus | Cobertura |
|-------|-----------|
| `BibliotecaTest` | Suite principal plain-Java (legacy, estable) |
| `BibliotecaJUnit5Test` | Validació, CRUD, filtres, estadístiques |
| `test/domini/` | Tests unitaris per a `Llibre`, `LlibreFilter`, `SortSpec`, `Llista`, `Tag` |
| `test/persistencia/` | `LlibreFieldBindings`, mapejadors de files, camps pesats, migracions |
| `test/herramienta/` | `ValidadorLlibre`, `Isbn13Normalizer`, CSV, JSON, i18n |
| `test/presentacio/` | Swing (headless-compatible) |
| `checkBiblio/StressTest` | Proves de càrrega i caos (1.7k+ línies) |
| `checkBiblio/UIAudit` | Auditoria automàtica de la GUI (Java Robot) |

> Per a proves d'estrès amb interfície gràfica cal un entorn amb X11 (o Xvfb a Linux headless).

---

## Configuració

Tots els ajustos es guarden a `~/.biblioteca/config.properties` i es gestionen des del diàleg de configuració de l'aplicació:

- Geometria i posició de la finestra
- Amplades de cada columna de la taula
- Tema actiu (clar, fosc, sepia, ocean)
- Mida de lletra (8 nivells predeterminats)
- Idioma de la interfície
- Tipus de base de dades, host, usuari i contrasenya

L'aplicació desa la configuració de forma asíncrona (`ScheduledExecutorService` amb 300 ms de debounce) i fa `flush` abans de tancar.

---

## Estructura del projecte

```
.
├── src/
│   ├── main/          Punt d'entrada (Executable.main)
│   ├── presentacio/   Panells Swing + controls (BookActions, Filter, ContextMenu, ...)
│   ├── domini/        Llibre, LlibreFilter, SortSpec, Llista, Tag, ControladorDomini
│   │   └── facade/    BookDelegate, ShelfDelegate, StatsDelegate, BackupDelegate, ...
│   ├── persistencia/  ControladorPersistencia, ConnexioServidor, DAO, migracions
│   ├── herramienta/   Config, UITheme, I18n, ValidadorLlibre, ImportadorLlibres, CoverService
│   └── listener/      Interfícies de callback (EnActualitzarBBDD, LibraryEvents)
├── build/deps.lock    Manifest de dependències (versió + SHA-256) — font de veritat
├── lib/               Jars descarregats (no versionats; `make deps`)
├── test/              Suite de tests
├── scripts/           Eines: fetch_deps.py, gen_strings.py, h2compat/
├── checkBiblio/       Proves d'estrès + audit d'UI
├── Makefile           build, test, run, deps, fuzz, package
└── install.sh         Empaquetat Linux (.deb, .AppImage)
```

---

## Còpies de seguretat i compatibilitat

- **Còpia SQL**: `Ctrl+B` crea un volcat amb totes les taules. La restauració
  fa una còpia de seguretat prèvia i, si el fitxer no és una còpia vàlida
  (buit, truncat o d'un altre format), **no es toca la base de dades**.
- **Les portades no s'inclouen** al volcat SQL (sóno blobs fora de l'export
  textual). Després de restaurar, cal tornar a descarregar-les.
- **Compatibilitat H2**: una base de dades creada amb H2 2.3.232 s'obre amb
  la versió actual sense perdre dades; `make h2-compat` ho comprova sobre una
  còpia temporal. El canvi de versió del motor és irreversible: després
  d'obrir la base de dades amb la versió nova, no es pot tornar enrere.

---

## Contribuir

Les contribucions són benvingudes! Alguns punts d'entrada:

1. **Errors i millores** — obre una *issue* amb passos per reproduir
2. **Traduccions** — afegeix un nou fitxer `strings_xx.properties` a `src/herramienta/`
3. **Tests** — la cobertura és bona però sempre pot ser millor; un test per *bug fix* és ideal

Abans d'enviar un *pull request*:

```bash
make compile    # ha de compilar sense warnings nous
make test       # tots els tests han de passar
```

---

## Llicència

Aquest projecte es distribueix sota la **GNU General Public License v3.0**.  
Consulta el fitxer [LICENSE](LICENSE) per als detalls complets.

<div align="center">

Fet amb Java 25 · Swing · H2 / MariaDB · OpenLibrary

</div>
