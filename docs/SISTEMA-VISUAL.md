# Sistema visual de Biblioteca

Guia per construir pantalles noves que quedin coherents amb la resta de
l'aplicació. Tot el que es descriu aquí és **obligatori**: si una pantalla
nova no el segueix, es veurà que no pertany a la mateixa aplicació.

---

## 1. On viu cada cosa

| Què | On | Per què |
|---|---|---|
| Colors semàntics | `herramienta/ui/UITheme.Paleta` | Un registre immutable amb els rols (`accent`, `textDark`, `bgPanel`…). Cap component defineix colors literals. |
| Mides i espaiat | `herramienta/ui/Tokens` | Escala d'espaiat, radis, alçades de control, amplada de lectura. |
| Aplicació dels tokens | `herramienta/ui/UITheme.aplicarUIManager()` | Projecta la paleta sobre les claus del `UIManager` que llegeix FlatLaf. |
| Components amb estil | `presentacio/util/UIComponents` | Fabrique de botons, camps, targetes i colors d'estat. |
| Icones | `presentacio/util/Icones` + `src/presentacio/icones/*.svg` | Família SVG local, rasteritzada i posada en memòria cau. |

**Regla d'or**: un component no ha de conèixer cap valor concret de color ni
de mida. Crida `UITheme.palette()` o `Tokens`. Si cal un color nou, s'afegeix
un rol nou a la paleta, no un literal al component.

---

## 2. Tokens

### 2.1 Colors

Es defineixen **per rol**, mai per aspecte. Els rols disponibles:

* Superfícies: `bgMain` (fons de la finestra), `bgPanel` (superfície de
  panell), `bgElevated` (targetes, diàlegs), `bgSunken` (pous de camp,
  barra de progrés), `bgSubtle` (files alternes).
* Text: `textDark` (principal), `textMid` (secundari), `textMuted` (pista),
  `textDisabled`, `textOnAccent` (sobre l'accent).
* Vores: `borderClr` (normal), `borderStrong` (marcada), `borderFocus`.
* Accent: `accent`, `accentHover`, `accentPressed`, `accentSoft` (fons
  seleccionat), `accentBorder`.
* Estat: `danger`, `dangerHover`, `dangerSoft`, `green`, `greenSoft`,
  `warning`, `warningSoft`.
* Interacció: `selectionBg`, `selectionText`, `hoverBg`, `focusRing`.
* Barra lateral: `sidebarBg`, `sidebarAccent`, `sidebarHoverBg`,
  `sidebarText`, `sidebarTextMid`, `sidebarSelBg`, `sidebarSelText`.

**Color de la paleta per defecte (LIGHT)**: blau/indi `#3358D4` d'accent,
fons `#F4F5F7`, superfícies `#FFFFFF`, text `#141A26`. El vermell
(`#C62828`) està **reservat a errors i accions destructives**.

Hi ha quatre temes: `LIGHT`, `DARK`, `SEPIA`, `OCEAN`. Tots tenen els mateixos
rols, de manera que un component nou no ha de saber quin tema està actiu.

### 2.2 Mides

* Espaiat: `4, 8, 12, 16, 24, 32` (`SPACE_1`…`SPACE_6`).
* Radis: `4, 6, 8` (`RADIUS_SM`, `RADIUS_MD`, `RADIUS_LG`). El valor per
  defecte de FlatLaf és `6`.
* Alçada de control: `34` (`CONTROL_HEIGHT`). **Mínima**, no fixa: el
  control ha de poder créixer amb la mida de lletra.
* Text base: `14` punts; títol de vista `20`; text auxiliar `12`.
* Amplada de lectura dels formularis: `560` (`FORM_READ_WIDTH`).

### 2.3 Escala de lletra

`Tokens.escalaLletra(px)` retorna el factor d'escala. Les mides de control
s'escalen amb `Tokens.escalaLletra(pxLletra, midaBase)`, que limita el
factor a `[0.85, 1.6]`: una lletra molt gran no ha de fer els controls
desproporcionats ni una lletra petita il·legible.

---

## 3. Tipografia

* Familia del sistema (`SansSerif`), mai una font descarregada.
* Pesos: normal per al text, **bold** per títols, encapçalaments i l'element
  de navegació actiu.
* Mai més de dos pesos en una mateixa vista.

---

## 4. Icones

* Viuen a `src/presentacio/icones/*.svg` (24×24, traç de 1.8, cantonada
  arrodonida).
* Es carreguen amb `Icones.icon(nom, mida, color)`; la mida per defecte és
  `16` (`Tokens.ICON_SIZE`).
* Els icones es rasteritzen **una vegada** per (nom, mida, color) i es
  posen en memòria cau. En canviar de tema cal cridar `Icones.invalidar()`.
* Els controls que només tenen icona porten `setAccessibleName()`: el
  tooltip no substitueix l'etiqueta accessible.

Noms disponibles: `biblioteca`, `llibres`, `recent`, `en-curs`, `llegits`,
`desitjos`, `prestecs`, `prestatgeries`, `etiquetes`, `estadistiques`,
`configuracio`, `mes`, `cerca`, `filtre`, `ordenacio`, `galeria`, `taula`,
`afegir`, `aleatori`, `importar`, `exportar`, `imprimir`, `editar`,
`esborrar`, `reintentar`, `tancar`, `fletxa-abaix`, `avis`, `info`, `ajuda`,
`enllaç`, `estrella`, `carpeta-mes`.

---

## 5. Components

### 5.1 Botons

| Paper | Mètode | Ús |
|---|---|---|
| Primari | `UIComponents.styleAccentButton(b)` | Una sola acció principal per pantalla. |
| Secundari | `UIComponents.styleSecondaryButton(b)` | Accions habituals. |
| Destructiu | `UIComponents.styleDangerButton(b)` | Només esborrats i accions irreversibles. |
| Commutador | `UIComponents.styleToggleButton(b)` | Filtres, modes. |

No se substitueix el `UI` del botó: FlatLaf ja dibuixa el focus, el hover i
l'estat deshabilitat. El component només fixa el color de fons i de text.

**Important**: no fixis la mida preferida d'un botó. Si cal una alçada
mínima, usa `UIComponents.setAlçadaControl(b, alçada)`, que només fixa el
*mínim* i deixa que el Look & Feel calculi l'amplada a partir del text.

### 5.2 Formularis

Construïr amb `presentacio.formularis.ConstructorGraellaFormulari`
(`GridBagLayout`):

```java
ConstructorGraellaFormulari b = ConstructorGraellaFormulari.columnsOf(form);
b.separador(I18n.t("form_section_identity"));
b.afegirField(I18n.t("field_isbn"), textISBN);
b.afegirCombo(I18n.t("field_format"), comboFormat);
b.afegirArea(I18n.t("field_notes"), scrollNotes);
```

* `afegirField` / `afegirCombo` / `afegirCheck`: control d'una línia. **No
  creix en alçada** quan el diàleg s'enlargeix.
* `afegirArea`: l'única peça que pot absorbir l'alçada extra.
* `separador`: encapçalament de secció.

** Mai utilitzis `GridLayout` en un formulari**: dona la mateixa mida a
totes les cel·les i els camps d'una línia creixen fins a esdevenir camps
gegants.

### 5.3 Estat buit

Cap component ha de deixar un rectangle bui. Un estat buit té:

1. un text que explica què passa,
2. una acció per resoldre'l.

Exemple: la barra lateral, sense prestatgeries, mostra
"Encara no tens cap prestatgeria…" i el botó "Crea la primera", que delega
a la gestió de llistes.

---

## 6. Patrons de pantalla

### 6.1 Finestra principal

```
┌──────────┬──────────────────────────────────────────┐
│ Barra    │ Capçalera: títol de vista + recompte    │
│ lateral  ├──────────────────────────────────────────┤
│ (nav +   │ Barra d'eines: cerca, filtres, ordre,   │
│ prestat.)│ vista, accions                           │
│          ├──────────────────────────────────────────┤
│          │ Contingut: galeria o taula               │
│          ├──────────────────────────────────────────┤
│          │ Barra d'estat                            │
└──────────┴──────────────────────────────────────────┘
```

* La capçalera mostra **el títol de la vista i el recompte**, que són
  l'única font del recompte (`EstatVistaBiblioteca.obtenirLlibresEnAmbit()`).
  La barra d'estat repeteix el mateix text: mai dos comptadors diferents.
* La selecció de la barra lateral es marca amb fons tou **i** indicador
  vertical de color accent **i** text en pes fort.
* Les entrades de navegació només existeixen si hi ha una vista o una acció
  real. Elsoni i les etiquetes es resolen dins el llibre i el filtre, i no
  apareixen com a opcions de navegació.

### 6.1.1 Barra lateral: columna única

Tota la barra lateral comparteix **un sol marge esquerre** (`Tokens.SPACE_4`
= 16 px): icones de navegació, icona de la marca, etiquetes de secció i
estat buit comencen a la mateixa columna. Les etiquetes de text, en canvi,
comencen on acaba la icona (`16 + 16 + 12`).

Regles de construcció, perquè no torni a trencar-se:

* **En una caixa vertical (`BoxLayout`), declara `alignmentX` a tots els
  fills.** `BoxLayout` calcula l'alineació horitzontal del contenidor a
  partir de la mitjana dels fills: un sol fill centrat (`alignmentX` per
  defecte = 0.5) desplaça tota la resta de la columna i deixa les files
  alineades a la dreta en lloc de l'esquerra.
* Els blocs de la barra (navegació, llistes) comparteixen **un únic área
  desplaçable**: el bloc d'accions queda fix a la part inferior, però la
  navegació i les llistes es desplacen plegades, de manera que una finestra
  baixa no retalla el contingut pel mig.
* Els blocs que no s'han d'estirar (llista de prestatgeries, estat buit)
  fixen la seva alçada màxima a la preferida: altrament ocupen tota la
  columna i allunyen el text de l'estat buit del seu títol.

### 6.1.2 Botó de tema

El botó de tema de la barra lateral **commuta** entre mode clar i fosc, i
el seu icono ensenya l'estat de destinació (lluna per passar a fosc, sol per
tornar a clar). El text del botó no du cap glif: la icona SVG ja porta el
significat, i dos glifs en un sol element es competeixen.

### 6.1.3 Canvi de tema: què s'ha de refrescar

Canviar de tema no és canviar quatre colors: és tornar a pintar **tota** la
finestra. Dues regles, perquè no torni a passar:

* **Cap color pot quedar congelat en el moment de construir el component.**
  Si un component desa un color en un camp (el peu d'una targeta, el títol
  de la marca, el fons d'un botó), en mode fosc continuarà amb el color del
  tema clar. Els colors es resolen en el moment de pintar, o bé es refresquen
  explícitament en el `aplicarTheme()` del component.
* **Les imatges pre-renderitzades que depenguin del tema s'han de
  reinvalidar.** La galeria desa a memòria cau el fons de la targeta i el
  marcador de portada: tots dos duen colors del tema i s'han de tornar a
  fer quan el tema canvia. Les imatges que **no** depenen del tema (les
  cobertes dels llibres, que són fotos) no s'han de buidar: buidar-les
  obliga a tornar-les a baixar totes.

I el canvi de tema ha de ser ** immediat**: reconstruir centenars de
targetes bloqueja el fil d'interfície i la finestra queda a mitges pintar,
amb zones del tema antic i zones del tema nou alhora. Si en el canvi de tema
cal reconstruir alguna cosa, la construcció ha de ser petita.

Després d'aplicar el tema, la finestra sencera es repinta: `invalidate()`,
`validate()` i `repaint()` sobre la finestra, no només sobre el panell fill.

### 6.2 Selector de vista

`presentacio.panells.SelectorSegmentat` és el patró per a un selector
exclusiu (Galeria / Taula). Mostra **quin** mode és actiu, no quin s'activarà
en prémer.

### 6.3 Targeta de galeria

* Portada a dalt (proporció conservada, mai deformada).
* Títol: com a molt dues línies.
* Autor: línia secundària.
* Valoració: només si el llibre en té (una xifra, no estrelles buides).
* Estat: etiqueta textual sobre la portada ("LLEGIT", "EN CURS"), no un punt
  de color.
* Progrés: text de pàgines + barra de 3 px.
* Sense portada: superfície neutra amb la icona de llibre al centre. **No**
  hi ha degradat ni el títol repetit dins de la portada.
* Amplada inicial 190 px, ajustable amb `Ctrl`+`±`.

### 6.4 Taula

* Files de 40 px, separadors horitzontals subtils, **sense** graella
  vertical.
* El títol és la columna expansible; l'autor té amplada intermitja; l'any,
  l'estat, el preu i l'ISBN són compactes.
* La capçalera de la columna de portada és buida (no hi cap text).
* El progrés és un renderer propi: text de pàgines i barra fina.
  Sense pàgines vàlides mostra "—"; amb pàgines i zero llegides mostra
  "0 / 320" (un 0% real, no una manca de dades).

### 6.5 Diàlegs de formulari

* Capçalera amb el títol i, si escau, l'acció principal.
* Cos desplaçable, **peu fix** fora del desplaçament amb les accions.
* Metadades secundàries en pestanyes.
* Formulari amb amplada de lectura limitada.
* Cap acció destructiva com a botó gran al costat de l'acció principal: la
  zona destructiva va separada, amb vora i confirmació explícita.

---

## 7. Accessibilitat

* Contrast: el text principal ha de tenir una raó de contrast ≥ 4.5 sobre el
  seu fons i el secundari ≥ 3.0 (WCAG AA). Ho comprova
  `test/herramienta/ui/TestUITheme`.
* Cap estat es comunica **només** amb color: hi ha text, forma o vora.
* Navegació completa amb teclat; l'indicador de focus és visible.
* Els controls amb icona tenen `setAccessibleName()`.
* El tooltip no substitueix una etiqueta essencial.
* L'escala de lletra ha de respectar-se: cap mida fixa que talli el text.

---

## 8. Canvas de proves visuals

`checkBiblio/ProvaVisual` arrenca l'aplicació real contra una base de dades
H2 en memòria, hi posa llibres ficticis i en captura la finestra en tema
clar i fosc, en galeria i en taula, i els diàlegs.

```bash
make compile
CP="bin:$(ls lib/*.jar | tr '\n' ':')"
javac -cp "$CP" -d bin checkBiblio/SuportTestUi.java checkBiblio/CapturaPantalla.java checkBiblio/ProvaVisual.java
xvfb-run -a -s "-screen 0 1600x1000x24" java -cp "bin:$CP" checkBiblio.ProvaVisual build/captures
```

La captura es fa pintant l'arbre de components damunt d'una imatge en memòria
(no amb `Robot`): és determinista i no depèn del gestor de finestres.
