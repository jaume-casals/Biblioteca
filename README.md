# Biblioteca

Gestor personal de biblioteca construït amb Java i Swing. Organitza els teus llibres en prestatges, puntua el que has llegit i obté metadades directament des d'OpenLibrary.

---

## Funcionalitats

- **Catàleg de llibres** — afegeix, edita i elimina llibres amb títol, autor, ISBN, preu, valoració i portada
- **Prestatges** — organitza els llibres en prestatges amb valoració i estat de lectura propis per prestatge
- **Cerca i filtres** — cerca en temps real per títol, autor, ISBN, descripció i notes; filtra per valoració, estat de lectura i prestatge
- **Portades** — carrega des d'un fitxer local o obté automàticament des d'OpenLibrary per ISBN
- **Importació / Exportació** — importació i exportació CSV, còpia de seguretat SQL i restauració amb còpia automàtica a l'inici
- **Estadístiques** — mitjanes per prestatge, percentatge de llegits i recompte de llibres
- **Mode fosc** — tema clar/fosc complet amb mida de lletra configurable
- **Dreceres de teclat** — `Ctrl+N` nou llibre, `Ctrl+F` cerca, `Ctrl+E` editar, `Ctrl+A` seleccionar tot, `Supr` eliminar, `Intro` obrir detalls
- **Préstecs** — registra a qui has prestat un llibre i marca'l com a retornat
- **Progrés de lectura** — camp de pàgines llegides amb barra de progrés a la taula

---

## Requisits

- Java 11 o superior
- Pantalla gràfica (X11 a Linux, natiu a macOS/Windows)

No cal instal·lació. Totes les dependències estan incloses a `lib/`.

---

## Execució

```bash
# Compilar i executar
make run

# Executar sense recompilar (bin/ ha d'existir)
make run-only

# O executar el jar directament
java -jar biblioteca.jar
```

---

## Compilació des del codi font

```bash
make compile          # compila src/ → bin/
make test             # compila + executa tots els tests (37 han de passar)
make clean            # elimina bin/
```

Classpath manual:

```bash
java -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar main.Ejecutable
```

---

## Base de dades

Utilitza **H2 embeguda** per defecte — no cal cap configuració. La base de dades es troba a `~/.biblioteca/biblioteca`.

Per canviar a **MariaDB**, edita `~/.biblioteca/config.properties`:

```properties
dbType=mariadb
dbHost=localhost
dbUser=usuari
dbPassword=contrasenya
```

Les migracions d'esquema s'executen automàticament a l'inici.

---

## Poblar amb dades d'exemple

```bash
make populate          # obté 2000 llibres d'OpenLibrary
make populate MAX=500  # límit personalitzat
```

---

## Estructura del projecte

```
src/
  main/          Punt d'entrada
  presentacio/   GUI Swing — panells (disseny) + controls (lògica), un parell per pantalla
  domini/        Lògica de negoci — llista en memòria ordenada de Llibre
  persistencia/  Capa JDBC — ControladorPersistencia → ServerConect
  herramienta/   Utilitats — Config, UITheme, LlibreValidator, FiltreUtils, AutoCompletion
  interficie/    Interfície de callback EnActualizarBBDD
lib/
  h2-2.3.232.jar
  mariadb-java-client-3.3.3.jar
test/
  BibliotecaTest.java
```

---

## Configuració

Tots els ajustos es guarden a `~/.biblioteca/config.properties` i es gestionen des del diàleg de configuració de l'aplicació. Inclou geometria de finestra, amplades de columnes, mode fosc, mida de lletra i connexió a la base de dades.

---

## Llicència

Aquest projecte es distribueix sota la llicència **GNU General Public License v3.0**.  
Consulta el fitxer [LICENSE](LICENSE) per als detalls complets.
