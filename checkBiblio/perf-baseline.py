#!/usr/bin/env python3
"""Construeix la base de mesura del banc de rendiment.

Reparteix una còpia del working tree i hi reverteix NOMÉS les optimitzacions
de rendiment d'aquesta tasca, de manera que el benchmark pugui comparar la
base (codi sense optimitzar) amb l'estat actual sense tocar el repositori.

Què es reverteix (i només això):

* ``src/persistencia/internal/LlibreMapper.java`` → versió d'origen: els
  accessors JDBC tornen a resoldre la columna pel nom en cada fila.
* ``src/persistencia/dao/LlibreSearchDao.java`` → versió d'origen: la
  consulta torna a projectar els camps pesats i a fer ``SELECT DISTINCT``.
* ``src/domini/Llibre.java`` → versió d'origen: es treu
  ``posarAutorsDeText`` (tornen el ``split`` amb expressió regular i el
  ``String.join`` de tornada).
* ``src/persistencia/dao/LlibreDaoCore.java`` → es reverteixen els punts
  d'optimització (índexs de columna per consulta, autorats agrupats, DELETE
  omitit en la inserció) i es deixa intacta la feina en curs del working
  tree (validació de còpies de seguretat, galeria de cobertes, i18n).

Ús::

    python3 checkBiblio/perf-baseline.py <directori-copia>
"""
import re
import subprocess
import sys
from pathlib import Path

# Els tres fitxers que no tenien feina en curs al working tree es recuperen
# directament de la base de l'stream de Git.
ORIGINAL_FROM_GIT = [
    "src/domini/Llibre.java",
    "src/persistencia/dao/LlibreSearchDao.java",
    "src/persistencia/internal/LlibreMapper.java",
]


def git_show(rev: str, path: str) -> str:
    return subprocess.run(
        ["git", "show", f"{rev}:{path}"],
        check=True, capture_output=True, text=True,
    ).stdout


def revert_llibre_dao_core(text: str) -> str:
    original = text

    # 1. Imports afegits per la resolució agrupada dels ids d'autor.
    text = text.replace("import java.util.HashMap;\n", "")
    text = text.replace("import java.util.Map;\n", "")

    # 2. obtenirAll(): índexs de columna per consulta i text d'autors.
    text = text.replace(
        """                // Els indexs de columna es resolen una vegada per consulta,
                // no per fila: resoldre'ls dins del bucle tornaria a pagar
                // la cerca de nom de columna del driver per cada llibre.
                LlibreMapper.Cols cols = new LlibreMapper.Cols(rs);
                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibreLight(rs, cols);""",
        """                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibreLight(rs);""",
    )

    # 3. obtenirAllComplets() i obtenirRecentlyAdded().
    text = text.replace(
        """                LlibreMapper.Cols cols = new LlibreMapper.Cols(rs);
                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibre(rs, cols);""",
        """                while (rs.next()) {
                    Llibre l = LlibreMapper.buildLlibre(rs);""",
    )
    text = text.replace(
        """                LlibreMapper.Cols cols = new LlibreMapper.Cols(rs);
                while (rs.next()) result.add(LlibreMapper.buildLlibre(rs, cols));""",
        """                while (rs.next()) result.add(LlibreMapper.buildLlibre(rs));""",
    )

    # 4. El text d'autors torna a dividir-se i concatenar-se.
    text = text.replace(
        "                        l.posarAutorsDeText(autor);",
        "                        l.posarAutors(java.util.Arrays.asList(autor.split(\", \", -1)));",
    )

    # 5. insert(): torna a esborrar els enllaços abans de sincronitzar.
    text = text.replace(
        """            List<String> autorsSync = resoldreAutorsSync(ll);
            // Es crea el llibre ara mateix: l'ISBN és clau primària, de manera
            // que l'INSERT només pot tenir èxit si el llibre era nou. Per
            // tant, no hi pot haver enllaços a llibre_autor que el referenciïn
            // i el DELETE previ és una operació garantidament buida: s'estalvia
            // una anada i tornada al servidor per cada llibre importat.
            if (!autorsSync.isEmpty()) sincronitzarAutors(ll.obtenirISBN(), autorsSync, false);""",
        """            List<String> autorsSync = resoldreAutorsSync(ll);
            if (!autorsSync.isEmpty()) sincronitzarAutors(ll.obtenirISBN(), autorsSync);""",
    )

    # 6. sincronitzarAutors(): torna a un SELECT i un INSERT per autor.
    start = text.index("    private void sincronitzarAutors(long isbn, List<String> autors) throws SQLException {")
    end = text.index("\n}", start)
    text = text[:start] + '''    private void sincronitzarAutors(long isbn, List<String> autors) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?")) {
            del.setLong(1, isbn);
            del.execute();
        }
        if (autors == null || autors.isEmpty()) return;
        executarBatchAutors(con, "INSERT IGNORE INTO autor (nom) VALUES (?)",
            (ps, nom) -> ps.setString(1, nom), autors);
        for (String nom : autors) {
            if (nom == null || nom.isBlank()) continue;
            Integer autorId = idAutorPerNom(nom);
            if (autorId == null) continue;
            MapejadorsFiles.exec(con, "INSERT IGNORE INTO llibre_autor (isbn, autor_id) VALUES (?, ?)",
                ps -> { ps.setLong(1, isbn); ps.setInt(2, autorId); });
        }
    }

    /** Id de l'autor amb aquest nom, o {@code null} si no existeix. */
    private Integer idAutorPerNom(String nom) throws SQLException {
        return MapejadorsFiles.queryOneWithParamsOrThrow(con,
            "SELECT id FROM autor WHERE nom = ?",
            ps -> ps.setString(1, nom),
            "Error buscant l'autor '" + nom + "'",
            rs -> rs.getInt(1));
    }''' + text[end:]

    if text == original:
        raise SystemExit("perf-baseline: no s'ha reverteix res a LlibreDaoCore.java")
    return text


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    root = Path(sys.argv[1]).resolve()

    base_rev = "ec2025e"
    for rel in ORIGINAL_FROM_GIT:
        (root / rel).write_text(git_show(base_rev, rel), encoding="utf-8")

    dao = root / "src/persistencia/dao/LlibreDaoCore.java"
    dao.write_text(revert_llibre_dao_core(dao.read_text(encoding="utf-8")), encoding="utf-8")

    # El fitxer de la còpia de seguretat ha de continuar sent el del working
    # tree: el que es reverteix són només les optimitzacions.
    print("perf-baseline: base construïda a", root)


if __name__ == "__main__":
    main()
