# Salt de línia a les cadenes traduïdes — convenció i estat

Document de referència per a la decisió pendent sobre com s'escriu un salt
de línia dins d'una cadena traduïda (`strings/*.csv` → `herramienta/i18n/I18n.java`
i `herramienta/strings_*.properties`).

## 1. Qui mana: el CSV

`strings/*.csv` és l'única font de veritat. El generador
(`scripts/gen_strings.py`) en produeix quatre artefactes:

| Artefacte | Generat | Llegeix l'aplicació? |
|---|---|---|
| `src/herramienta/i18n/I18n.java` | sí | **sí** — és la taula viva |
| `src/herramienta/strings_ca.properties` | sí | no (cap codi Java les llegeix) |
| `src/herramienta/strings_es.properties` | sí | no |
| `src/herramienta/strings_en.properties` | sí | no |

Per tant, **un canvi de text s'ha de fer al CSV i regenrar**; editar
`I18n.java` a mà es perd a la regenació següent.

### 1.1. La plantilla Java

`I18n.java` ja no és un artefacte "no regenerable". El codi estructural de
la classe (la taula `PLACEHOLDER_PATTERNS`, la substitució de placeholders
amb regex, `aplicarSwingOptionPane()`, la consulta de
`Configuracio.obtenirLang()` i l'avís de llengua desconeguda) viu a
`scripts/i18n_java_template.java.tmpl`, i el generador hi injecta les files
`T(...)` a la marca `//%%TABLE%%`.

Abans la plantilla era una cadena literal dins del mateix generador i
referenciava `Config.getLang()` i `applySwingOptionPane()` — dos noms que
no existeixen (els reals són `Configuracio.obtenirLang()` i
`aplicarSwingOptionPane()`). `make strings` produïa un fitxer que **no
compilava**, i la CI ho amagava amb `--no-java`: el gate només comparava els
`.properties` i deixava passar una taula Java obsoleta.

### 1.2. El gate

```
python3 scripts/gen_strings.py --check     # gate complet: I18n.java + .properties
python3 scripts/test_gen_strings.py        # invariants d'escapament
```

`make strings-check` i el job `i18n` de la CI executen tots dos. El gate
compara el fitxer **byte a byte** amb el que generaria el CSV: qualsevol
divergència (una taula editada a mà, un `.properties` desfasat) fa fallar
la CI.

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

### Convenció B — salt de línia real

Al CSV, el camp conté un salt de línia real dins de cometes:

```
dlg_restore_done_with_note,"{0}\n\n{1}",...
```

A `I18n.java` és `\n` (una sola barra invertida dins el literal Java), és a
dir un **salt de línia real en temps d'execució**.

## 3. On és la limitació real del pipeline

No és el transport, sinó el **renderitzament**: molts dels textos afectats
es mostren amb `JOptionPane.showMessageDialog` / `showConfirmDialog`, que
**no interpreta els salts de línia**; el text apareix en una sola línia. Per
mostrar un salt de línia cal un component que les interpreti (`JTextArea` no
editable dins d'un `JOptionPane`, o un `JLabel` amb `<html>`).

Per això la Convenció A (seqüència literal) i la Convenció B (salt real)
no es poden barrejar en un mateix diàleg sense que una de les dues formes
desaparesqui a la pantalla.

## 4. Convenció canònica proposada

**Un salt de línia és un salt de línia real dins del camp citat del CSV.**

Conseqüència per als textos existents: les claus que avui mostren `\n`
literal passen a mostrar un salt de línia. **És un canvi de text visible**
i, per això, requereix acord de producte.

Alternativa de migració zero-visible: mantenir la Convenció A a tots els
llocs i arreglar només els punts on el text s'ha de mostrar en varies línies,
substituint el `JOptionPane` per un diàleg que interpreti els salts. Els
textos amb `\n` literal seguirien mostrant-se literalment, cosa que sembla
un error de redacció.

## 5. Escapament: estat actual

El generador distingeix el salt de línia real de la seqüència literal `\n`
i de les cometes, i el fitxer generat sempre torna el valor en temps
d'execució exactament igual que el CSV (ho fixen les invariants de
`scripts/test_gen_strings.py`).

Aquesta separació ja no es trenca. El generador de la taula Java i el dels
`.properties` comparteixen el mateix escapador, de manera que la taula viva
i els `.properties` han de coincidir sempre.

Les claus que fins avui tenien **doble escapament** a la taula Java
(`\\\\n`, `\\\\\"`, és a dir un `\\n` i un `\\"` a la pantalla) s'han
corregit: el valor en temps d'execució ara és el del CSV, amb un sol nivell
de barra invertida.

| Clau | Abans (pantalla) | Ara (pantalla) |
|---|---|---|
| `dlg_confirm_galeria_delete_one` | `Eliminar \"{0}\"?\\nAquesta…` | `Eliminar "{0}"?\nAquesta…` |
| `dlg_confirm_galeria_delete_n` | `Eliminar {0} llibres…\\nAquesta…` | `Eliminar {0} llibres…\nAquesta…` |
| `dlg_undo_done` | `\"{0}\" restaurat.` | `"{0}" restaurat.` |
| `dlg_rename_llista_prompt` | `Nou nom per a la llista \\\"{0}\\\":` | `Nou nom per a la llista "{0}":` |
| `dlg_perfil_confirmar` | `Canviar al perfil \\\"{0}\\\"?…` | `Canviar al perfil "{0}"?…` |
| `dlg_no_llistes_msg` | `… des de \\\"Gestionar llistes\\\".` | `… des de "Gestionar llistes".` |
| `dlg_perfil_nom_invalid` | `… Es desarà com a \\\"{0}\\\".…` | `… Es desarà com a "{0}".…` |

`dlg_perfil_nom_invalid` era un cas diferent: la barra invertida sobrant
era al **CSV** (`\""{0}\""`), de manera que ni el generador ni la taula
Java la podien normalitzar. S'ha corregit al CSV.

## 6. Estat de les comprovacions

| Comanda | Estat |
|---|---|
| `python3 scripts/gen_strings.py --check` (gate de la CI) | **PASS** (I18n.java + `.properties`) |
| `python3 scripts/test_gen_strings.py` | **PASS** (9 invariants) |
| `make strings` | regenera els quatre artefactes |
| `make strings-check` | gate complet |

## 7. Bloquejat (requereix acord de producte)

* migrar les claus que avui mostren `\n` literal a la Convenció B;
* decidir quins diàlegs passen a interpretar els salts de línia.
