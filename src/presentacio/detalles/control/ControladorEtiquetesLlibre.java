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
import interficie.EscritorEtiqueta;
import herramienta.DialegError;
import herramienta.I18n;
import presentacio.detalles.vista.DialegEtiquetesLlibre;

public class ControladorEtiquetesLlibre {

    // NOTA DE DISSENY: TagsDelLlibreDialog aplica els canvis de pertinença
    // d'etiquetes immediatament (addLlibreToTag / removeLlibreFromTag es
    // persisteixen a cada clic), mentre que DialegLlistesLlibre ajorna
    // tots els canvis fins que l'usuari fa clic a "Desar". Aquest enfocament
    // de persistència immediata s'escull perquè les operacions d'etiquetes
    // són lleugeres (una sola fila de pertinença N:M) i els usuaris esperen
    // un feedback instantani. La pertinença a prestatgeries comporta
    // valoració i estat de lectura per llibre que es beneficien de
    // l'edició per lots, i per això les prestatgeries fan servir desat diferit.

    private final DialegEtiquetesLlibre vista;
    private final Llibre llibre;
    private final EscritorEtiqueta cd;
    private ArrayList<Tag> tagsCache = new ArrayList<>();
    private ArrayList<Tag> allTagsCache = new ArrayList<>();
    private ArrayList<Tag> displayedTags = new ArrayList<>();
    /** Mapa en caché d'etiqueta → recompte de llibres. S'omple un sol cop
     *  amb {@link #computeTagCounts()} i es manté sincronitzat incrementalment
     *  amb {@link #bumpCount} / {@link #dropCount} a addLlibreToTag /
     *  removeLlibreFromTag. Evita la consulta SQL de 10k files per
     *  recàrrega que el finding LOW de tot.txt va assenyalar. */
    private Map<Integer, Integer> tagCounts = new HashMap<>();

    public ControladorEtiquetesLlibre(DialegEtiquetesLlibre vista, Llibre llibre, EscritorEtiqueta cd) {
        this.vista = vista;
        this.llibre = llibre;
        this.cd = cd != null ? cd : domini.ControladorDomini.getInstance();
        wireListeners();
        reload();
    }

    private void wireListeners() {
        vista.obtenirBtnCrear().addActionListener(e -> onCrear());
        vista.obtenirBtnAfegir().addActionListener(e -> onAfegir());
        vista.obtenirBtnTreure().addActionListener(e -> onTreure());
        vista.obtenirTxtNovaEtiqueta().addActionListener(e -> onCrear());
        vista.obtenirFilterField().getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrarTags(); }
            public void removeUpdate(DocumentEvent e) { filtrarTags(); }
            public void changedUpdate(DocumentEvent e) { filtrarTags(); }
        });
    }

    private void onCrear() {
        String nom = vista.obtenirTxtNovaEtiqueta().getText().trim();
        if (nom.isEmpty()) return;
        try {
            cd.afegirTag(nom);
            vista.obtenirTxtNovaEtiqueta().setText("");
            allTagsCache = new ArrayList<>(cd.obtenirAllTags());
            reloadComboAdd();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onAfegir() {
        if (vista.obtenirComboAdd().getItemCount() == 0) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_no_tags_msg"),
                I18n.t("dlg_no_tags_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Tag tag = (Tag) vista.obtenirComboAdd().getSelectedItem();
        if (tag == null) return;
        if (tagsCache.stream().anyMatch(t -> t.obtenirId() == tag.obtenirId())) {
            JOptionPane.showMessageDialog(vista,
                I18n.t("dlg_tag_duplicate", tag.obtenirNom()),
                I18n.t("dlg_duplicate_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            cd.afegirLlibreToTag(llibre.obtenirISBN(), tag.obtenirId());
            bumpCount(tag.obtenirId());
            reload();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
        }
    }

    private void onTreure() {
        int row = vista.obtenirTable().getSelectedRow();
        if (row < 0 || row >= displayedTags.size()) return;
        Tag target = displayedTags.get(row);
        try {
            cd.eliminarLlibreFromTag(llibre.obtenirISBN(), target.obtenirId());
            dropCount(target.obtenirId());
            reload();
        } catch (Exception ex) {
            new DialegError(ex).mostrarErrorMessage();
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

    private void filtrarTags() {
        String text = vista.obtenirFilterField().getText().trim().toLowerCase();
        if (text.isEmpty()) {
            displayedTags = new ArrayList<>(tagsCache);
            vista.actualitzarComboAdd(allTagsCache);
        } else {
            displayedTags = tagsCache.stream()
                .filter(t -> t.obtenirNom().toLowerCase().contains(text))
                .collect(Collectors.toCollection(ArrayList::new));
            ArrayList<Tag> filteredAll = allTagsCache.stream()
                .filter(t -> t.obtenirNom().toLowerCase().contains(text))
                .collect(Collectors.toCollection(ArrayList::new));
            vista.actualitzarComboAdd(filteredAll);
        }
        vista.actualitzarTable(displayedTags, tagCounts);
    }

    private void reload() {
        tagsCache = new ArrayList<>(cd.obtenirTagsForLlibre(llibre.obtenirISBN()));
        allTagsCache = new ArrayList<>(cd.obtenirAllTags());
        computeTagCounts();
        displayedTags = new ArrayList<>(tagsCache);
        reloadComboAdd();
        vista.actualitzarTable(displayedTags, tagCounts);
        vista.obtenirFilterField().setText("");
    }

    private void computeTagCounts() {
        tagCounts = new HashMap<>();
        for (LlibreTagRow row : cd.obtenirAllLlibreTagRows()) {
            tagCounts.merge(row.tagId(), 1, Integer::sum);
        }
    }

    private void reloadComboAdd() {
        vista.actualitzarComboAdd(allTagsCache);
    }
}