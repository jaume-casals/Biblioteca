# Salt de línia a les cadenes traduïdes — convenció i estat

Document de referència per a la decisió pendent sobre com s'escriu un salt
de línia dins d'una cadena traduïda (`strings/*.csv` → `herramienta/i18n/I18n.java`
i `herramienta/strings_*.properties`).

## 1. Les claus manuals (migrades)

`src/herramienta/i18n/I18n.java` **no és un artefacte regenerable tal com està**:
conté codi editat a mà a més de la taula de cadenes. Hi havia cinc claus
que no sortien de `strings/*.csv`:

| Clau | Línia a `I18n.java` | Observació |
|---|---|---|
| `dlg_restore_done_with_note` | 259 | valor amb salt de línia real |
| `tip_filter_substring` | 375 | valor amb salt de línia real |
| `dlg_fetch_portades_done_partial` | 416 | valor amb salt de línia real |
| `dlg_perfil_nom_invalid` | 516 | valor amb salt de línia real |
| `dlg_perfil_nom_invalid_title` | 517 | valor amb salt de línia real |

**Aquestes cinc claus ja han migrat a `strings/ui.csv`** amb el valor en temps
d'execució idèntic al que mostrava la taula viva (verificat fila a fila: el
generador reprodueix exactament la fila de `I18n.java` per a les cinc claus).
`dlg_restore_done_with_note` és el primer registre de diverses línies del CSV
(camp citat amb dos salts de línia reals).

Amb la migració, els quatre `FAIL` de l'auditoria d'i18n
(`clau I18n.t absent del CSV`) han desaparegut: l'informe passa de
`FAIL: 4` a `FAIL: 0` sense cap canvi de text visible.

A més, el fitxer conté codi que el generador no produeix: la taula
`PLACEHOLDER_PATTERNS`, la substitució de placeholders amb regex a `t()`,
`aplicarSwingOptionPane()`, la consulta de `Configuracio.obtenirLang()` i el
avís de llengua desconeguda.

**Conseqüència pràctica:** `make strings` (és a dir, `python3 scripts/gen_strings.py`
sense `--no-java`) **destrueix a mà la taula editada** i deixa el projecte sense
compilar. Cal regenerar només els `.properties` amb `--no-java`, que és el que
fa la CI.

## 2. Les dues convencions que conviuen avui

### Convenció A — seqüència literal `\n` (la majoria de les claus)

Al CSV, el text conté dos caràcters: barra invertida i `n`.

```
dlg_backup_done,Backup guardat a:\n{0},Copia guardada en:\n{0},Backup saved to:\n{0}
```

A `I18n.java` el valor és `\\n` (barra invertida escapada dins el literal
Java), de manera que **en temps d'execució la pantalla mostra els dos
caràcters `\n` literalment**, no un salt de línia. És el que veu l'usuari
avui: `Backup guardat a:\n/home/...`.

### Convenció B — salt de línia real (les cinc claus manuals)

Al CSV, el camp conté un salt de línia real dins de cometes:

```
dlg_restore_done_with_note,"{0}\n\n{1}",...
```

A `I18n.java` és `\n` (una sola barra invertida dins el literal Java), és a
dir un **salt de línia real en temps d'execució**. Aquesta és la convenció
que la interfície hauria de fer servir.

Hi ha una tercera situació, residual: sis claus tenen el doble d'escapament
a `I18n.java` (`\\\\n`, `\\\\\"`), de manera que en temps d'execució mostren
`\\n` i `\\"`. Són `dlg_confirm_galeria_delete_n`,
`dlg_confirm_galeria_delete_one`, `dlg_no_llistes_msg`,
`dlg_perfil_confirmar`, `dlg_rename_llista_prompt` i `dlg_undo_done`.

## 3. On és la limitació real del pipeline

No és el CSV. El pipeline Java (`Rfc4180Reader`, `UtilitatsCsv`) admet
registres de diverses línies i camps citats amb salts reals: el format ja
hi arriba. La limitació estava (i s'ha corregit) en el **generador**:

* `scripts/gen_strings.py` llegia el CSV línia a línia i, per tant, un camp
  citat amb salt de línia real es trencava en dos registres;
* l'escapador confonia el salt de línia real amb la seqüència literal
  `\n`: tots dos es convertien en `\n` (un salt real en temps d'execució),
  de manera que el generador **pnia convertir en silenci** un text amb
  `\n` literal en un text amb salt de línia.

Corregit el generador (vegeu `scripts/test_gen_strings.py`), el CSV sí que
pot portar un salt de línia real i les dues formes es mantenen separades fins
al valor final.

El que queda pendent no és el transport, sinó el **renderitzament**: molts
dels textos afectats s Mostren amb `JOptionPane.showMessageDialog`, que **no
interpreta els salts de línia**; el text apareix en una sola línia. Per
mostrar un salt de línia cal un component que les interpreti (`JTextArea` no
editable dins d'un `JOptionPane`, o un `JLabel` amb `<html>`).

## 4. Convenció canònica proposada

**Un salt de línia és un salt de línia real dins del camp citat del CSV.**

```
dlg_backup_done,"Backup guardat a:
{0}","Copia guardada en:
{0}","Backup saved to:
{0}"
```

Conseqüència per als textos existents: 590 de 590 les claus del CSV
passen a portar el salt real, i les 17 claus que avui mostren `\n` literal
passen a mostrar un salt de línia. **És un canvi de text visible** i, per
això, requereix acord de producte.

Alternativa de migració zero-visible: mantenir la convenció A (seqüència
literal) a tots els llocs i arreglar només els punts on el text s'ha de
mostrar en varies línies, substituint el `JOptionPane` per un diàleg que
interpreti els salts. Els textos amb `\n` literal seguirien mostrant-se
literalment, cosa que sembla un error de redacció.

## 5. Què s'ha fet i què queda bloquejat

**Fet (no canvia cap text visible):**

* `scripts/gen_strings.py` llegeix el CSV sencer, de manera que admet
  registres de diverses línies i camps citats amb salts reals;
* l'escapador distingeix el salt de línia real de la seqüència literal
  `\n`; amb això la sortida del generador **coincideix amb la taula
  editada a mà en 589 de les 595 claus del CSV** (abans de la correcció
  eren 584 de 590; les sis claus que queden són les que tenen un
  escapament doble a la taula editada, descrites al §2);
* les cinc claus manuals han migrat a `strings/ui.csv` amb el valor en
  temps d'execució idèntic → l'informe d'UIAudit passa de `FAIL: 4` a
  `FAIL: 0`;
* els tres `.properties` s'han regenerat: ara són coherents amb la taula
  Java, que és l'artefacte viu. Cap codi Java les llegeix (cap referència
  a `strings_*.properties` al codi), de manera que no hi ha cap efecte
  visible;
* l'auditoria d'i18n (`checkBiblio/I18nAudit.java`) llegeix els CSV per
  registres lògics (admés registres de diverses línies) i detecta l'ús
  indirecte de claus via `Camp.text(key, labelKey, tooltipKey, width)`;
* `scripts/test_gen_strings.py` fixa les invariants (salt real, seqüència
  literal, cometes, barres inverses, Unicode, placeholders als tres
  idiomes i registre CSV multilínia). Target: `make strings-test`.

**Bloquejat (requereix acord de producte):**

* migrar els 17 textos de la convenció A a la convenció B;
* decidir quins diàlegs passen a interpretar els salts de línia;
* les sis claus amb escapament doble a la taula editada
  (`dlg_confirm_galeria_delete_n`, `dlg_confirm_galeria_delete_one`,
  `dlg_no_llistes_msg`, `dlg_perfil_confirmar`, `dlg_rename_llista_prompt`
  i `dlg_undo_done`): el CSV hi posa un sol nivell de barra invertida i la
  taula editada en té dos, de manera que a la pantalla hi surt `\\n` i
  `\\"` en lloc de `\n` i `\"`. Corregir-ho canvia text visible, per això
  queda pendent de la mateixa decisió.

El generador tampoc s'ha fet segur encara: `python3 scripts/gen_strings.py`
sense `--no-java` continua destrueint les cinc claus manuals i el codi
estructural de `I18n.java`. Fins que es decideixi la convenció i es
traslladin les claus al CSV, el procediment segur és
`python3 scripts/gen_strings.py --no-java` (o `make strings-check` per
validar).

## 6. Estat dels avisos de generació

| Comanda | Estat |
|---|---|
| `python3 scripts/gen_strings.py --check --no-java` (gate de la CI) | **PASS** |
| `python3 scripts/gen_strings.py --check` (inclou `I18n.java`) | **FAIL preexistent**: `I18n.java` conté codi editat a mà (l'estructura del generador encara no el reprodueix) |
| `python3 scripts/test_gen_strings.py` | **PASS** (9 invariants) |
| `./checkBiblio/run.sh --audit-only --auto` (auditoria d'i18n) | **PASS**, `FAIL: 0` (abans: `FAIL: 4`) |
