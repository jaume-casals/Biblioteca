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

# Compila i executa
make run
```

### Windows

```bat
scripts\compile.bat
java -cp bin;lib\h2-2.3.232.jar;lib\mariadb-java-client-3.3.3.jar;lib\gson-2.11.0.jar main.Ejecutable
```

> Totes les dependències estan incloses a `lib/` — no cal `apt`, `brew` ni cap gestió de paquets externa.

### Instal·lador

També disponible un instal·lador gràfic per a Windows (`install.exe`) i un script de preparació per a Linux (`install.sh`).

---

## Compilació

| Objectiu | Comanda | Descripció |
|----------|---------|------------|
| Compilar | `make compile` | `src/` → `bin/` (Java 21) |
| Tests | `make test` | Executa tota la suite JUnit 5 |
| Netejar | `make clean` | Elimina `bin/` |
| Executar | `make run` | Neteja + compila + obre la GUI |
| Auditories | `./checkBiblio/run.sh` | Audit d'UI + tests d'estrès |

**Requisits:** JDK 21 o superior.

**Compilació manual** (sense Make):

```bash
find ./src/ -name "*.java" > classes.txt
javac --release 21 -cp lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar @classes.txt -d bin
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
│                 ServerConect, migracions, H2/MariaDB         │
└─────────────────────────────────────────────────────────────┘
```

| Capa | Responsabilitat | Tecnologies |
|------|-----------------|-------------|
| `presentacio/` | Panells i controls Swing, listeners, renderers | Java 21, Swing, Nimbus L&F |
| `domini/` | Regles de negoci, validació, filtres, ordenació | Records, lambdes, `StateContext` amb `synchronized` |
| `persistencia/` | Connexió JDBC, DAOs, migracions, transaccions | H2 / MariaDB, `PreparedStatement` |
| `herramienta/` | Utilitats compartides (Config, UITheme, I18n, Validator) | `java.util.logging`, `ResourceBundle` |
| `interficie/` | Interfícies de callback per a esdeveniments de domini | — |

L'estat en memòria viu a `StateContext` i es serialitza a la base de dades en cada mutació — el patró és **write-through**. Tots els mètodes de `ControladorPersistencia` són `synchronized`, cosa que evita la doble sincronització als DAOs.

---

## Tests

La suite utilitza **JUnit 5**, **AssertJ** i **Mockito** (tots inclosos a `lib/`). Cada grup de tests utilitza una base de dades H2 en memòria independent.

```bash
make test
```

**Estructura dels tests:**

| Tipus | Cobertura |
|-------|-----------|
| `BibliotecaTest` | Suite principal plain-Java (legacy, estable) |
| `BibliotecaJUnit5Test` | ~184 tests JUnit 5 — validació, CRUD, filtres, estadístiques |
| `test/domini/` | Tests unitaris per a `Llibre`, `LlibreFilter`, `SortSpec`, `Llista`, `Tag` |
| `test/persistencia/` | Tests per a `LlibreFieldBindings`, `RowMappers`, `ConnectionConfig` |
| `test/herramienta/` | Tests per a `LlibreValidator`, `Isbn13Normalizer`, `CsvUtils`, `I18n` |
| `test/presentacio/` | Tests per a `FilterDrawerPanel` i `TablePageController` |
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
│   ├── main/          Punt d'entrada (Ejecutable.main)
│   ├── presentacio/   Panells Swing + controls (BookActions, Filter, ContextMenu, ...)
│   ├── domini/        Llibre, LlibreFilter, SortSpec, Llista, Tag, ControladorDomini
│   │   └── facade/    BookDelegate, ShelfDelegate, StatsDelegate, BackupDelegate, ...
│   ├── persistencia/  ControladorPersistencia, ServerConect, DAO, migracions
│   ├── herramienta/   Config, UITheme, I18n, LlibreValidator, BookImporter, CoverService
│   └── interficie/    Interfícies de callback (EnActualizarBBDD, LibraryEvents)
├── lib/               Dependències empaquetades (H2, MariaDB, Gson, JUnit, Mockito)
├── test/              Suite de tests (gitignored)
├── checkBiblio/       Proves d'estrès + audit d'UI
├── Makefile           build, test, run, populate, audit
├── pom.xml            Metadades Maven
└── install.sh         Empaquetat Linux (.deb, .AppImage)
```

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

Fet amb Java 21 · Swing · H2 / MariaDB · OpenLibrary

</div>
