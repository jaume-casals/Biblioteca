package persistencia;

import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import persistencia.RelationRow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LibraryGraph {

    private final Map<Long, List<LlibreAutorRow>> autorsByIsbn = new HashMap<>();
    private final Map<Long, List<LlibreLlistaRow>> llistesByIsbn = new HashMap<>();
    private final Map<Long, List<LlibreTagRow>> tagsByIsbn = new HashMap<>();
    private List<LlibreAutorRow> allAutors = Collections.emptyList();
    private List<LlibreLlistaRow> allLlistes = Collections.emptyList();
    private List<LlibreTagRow> allTags = Collections.emptyList();

    public LibraryGraph() {
        this(ControladorPersistencia.getInstance());
    }

    public LibraryGraph(ControladorPersistencia cp) {
        load(cp);
    }

    private void load(ControladorPersistencia cp) {
        autorsByIsbn.clear();
        llistesByIsbn.clear();
        tagsByIsbn.clear();

        allAutors = cp.getAllLlibreAutorRows();
        for (LlibreAutorRow row : allAutors) {
            autorsByIsbn.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(row);
        }

        allLlistes = cp.getAllLlibreLlista();
        for (LlibreLlistaRow row : allLlistes) {
            llistesByIsbn.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(row);
        }

        allTags = cp.getAllLlibreTag();
        for (LlibreTagRow row : allTags) {
            tagsByIsbn.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(row);
        }
    }

    public void reload() {
        load(ControladorPersistencia.getInstance());
    }

    public List<LlibreAutorRow> getAutorsForIsbn(long isbn) {
        return autorsByIsbn.getOrDefault(isbn, Collections.emptyList());
    }

    public List<LlibreLlistaRow> getLlistesForIsbn(long isbn) {
        return llistesByIsbn.getOrDefault(isbn, Collections.emptyList());
    }

    public List<LlibreTagRow> getTagsForIsbn(long isbn) {
        return tagsByIsbn.getOrDefault(isbn, Collections.emptyList());
    }

    public List<RelationRow> getAllRows() {
        List<RelationRow> rows = new ArrayList<>(allAutors.size() + allLlistes.size() + allTags.size());
        rows.addAll(allAutors);
        rows.addAll(allLlistes);
        rows.addAll(allTags);
        return rows;
    }
}