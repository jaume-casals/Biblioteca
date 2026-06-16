package presentacio.detalles.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import domini.Llibre;
import persistencia.LlibreTagRow;
import domini.Tag;
import interficie.TagWriter;
import herramienta.DialogoError;
import herramienta.I18n;
import presentacio.detalles.vista.TagsDelLlibreDialog;

public class TagsDelLlibreControl {

    // DESIGN NOTE: TagsDelLlibreDialog applies tag membership changes immediately
    // (addLlibreToTag / removeLlibreFromTag are persisted on each click), whereas
    // LlistesDelLlibreDialog defers all changes until the user clicks "Save".
    // This immediate-persist approach is chosen because tag operations are
    // lightweight (a single many-to-many membership row) and users expect instant
    // feedback. Shelf membership also carries per-book rating/read-state that
    // benefits from batch editing, which is why shelves use deferred save.

    private final TagsDelLlibreDialog vista;
    private final Llibre llibre;
    private final TagWriter cd;
    private ArrayList<Tag> tagsCache = new ArrayList<>();
    private ArrayList<Tag> allTagsCache = new ArrayList<>();
    private ArrayList<Tag> displayedTags = new ArrayList<>();
    /** Cached tag→book-count map. Populated once via {@link #computeTagCounts()},
     *  then kept in sync incrementally by {@link #bumpCount} / {@link #dropCount}
     *  on addLlibreToTag / removeLlibreFromTag. Avoids the per-reload 10k-row
     *  SQL query the tot.txt LOW finding flagged. */
    private Map<Integer, Integer> tagCounts = new HashMap<>();

    public TagsDelLlibreControl(TagsDelLlibreDialog vista, Llibre llibre, TagWriter cd) {
        this.vista = vista;
        this.llibre = llibre;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        wireListeners();
        reload();
    }

    private void wireListeners() {
        vista.getBtnCrear().addActionListener(e -> onCrear());
        vista.getBtnAfegir().addActionListener(e -> onAfegir());
        vista.getBtnTreure().addActionListener(e -> onTreure());
        vista.getTxtNovaEtiqueta().addActionListener(e -> onCrear());
        vista.getFilterField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterTags(); }
            public void removeUpdate(DocumentEvent e) { filterTags(); }
            public void changedUpdate(DocumentEvent e) { filterTags(); }
        });
    }

    private void onCrear() {
        String nom = vista.getTxtNovaEtiqueta().getText().trim();
        if (nom.isEmpty()) return;
        try {
            cd.addTag(nom);
            vista.getTxtNovaEtiqueta().setText("");
            allTagsCache = new ArrayList<>(cd.getAllTags());
            reloadComboAdd();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onAfegir() {
        if (vista.getComboAdd().getItemCount() == 0) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_no_tags_msg"),
                I18n.t("dlg_no_tags_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Tag tag = (Tag) vista.getComboAdd().getSelectedItem();
        if (tag == null) return;
        if (tagsCache.stream().anyMatch(t -> t.getId() == tag.getId())) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_tag_duplicate", tag.getNom()),
                I18n.t("dlg_duplicate_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            cd.addLlibreToTag(llibre.getISBN(), tag.getId());
            bumpCount(tag.getId());
            reload();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void onTreure() {
        int row = vista.getTable().getSelectedRow();
        if (row < 0 || row >= displayedTags.size()) return;
        Tag target = displayedTags.get(row);
        try {
            cd.removeLlibreFromTag(llibre.getISBN(), target.getId());
            dropCount(target.getId());
            reload();
        } catch (Exception ex) {
            new DialogoError(ex).showErrorMessage();
        }
    }

    private void bumpCount(int tagId) {
        tagCounts.merge(tagId, 1, Integer::sum);
    }

    private void dropCount(int tagId) {
        tagCounts.merge(tagId, -1, Integer::sum);
        if (tagCounts.get(tagId) != null && tagCounts.get(tagId) <= 0) {
            tagCounts.remove(tagId);
        }
    }

    private void filterTags() {
        String text = vista.getFilterField().getText().trim().toLowerCase();
        if (text.isEmpty()) {
            displayedTags = new ArrayList<>(tagsCache);
            vista.updateComboAdd(allTagsCache);
        } else {
            displayedTags = tagsCache.stream()
                .filter(t -> t.getNom().toLowerCase().contains(text))
                .collect(Collectors.toCollection(ArrayList::new));
            ArrayList<Tag> filteredAll = allTagsCache.stream()
                .filter(t -> t.getNom().toLowerCase().contains(text))
                .collect(Collectors.toCollection(ArrayList::new));
            vista.updateComboAdd(filteredAll);
        }
        vista.updateTable(displayedTags, tagCounts);
    }

    private void reload() {
        tagsCache = new ArrayList<>(cd.getTagsForLlibre(llibre.getISBN()));
        allTagsCache = new ArrayList<>(cd.getAllTags());
        computeTagCounts();
        displayedTags = new ArrayList<>(tagsCache);
        reloadComboAdd();
        vista.updateTable(displayedTags, tagCounts);
        vista.getFilterField().setText("");
    }

    private void computeTagCounts() {
        tagCounts = new HashMap<>();
        for (LlibreTagRow row : cd.getAllLlibreTagRows()) {
            tagCounts.merge(row.tagId(), 1, Integer::sum);
        }
    }

    private void reloadComboAdd() {
        vista.updateComboAdd(allTagsCache);
    }
}