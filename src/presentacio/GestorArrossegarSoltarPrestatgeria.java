package presentacio;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JButton;

/** Adjunta les destinacions d'arrossegar a prestatgeria; manté la lògica de DnD fora de {@link PanellBarraEsquerra}. */
final class GestorArrossegarSoltarPrestatgeria {

    private GestorArrossegarSoltarPrestatgeria() {}

    static void attach(JButton shelfBtn, int shelfId, BiConsumer<Integer, List<Long>> onDrop) {
        new DropTarget(shelfBtn, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent ev) {
                try {
                    ev.acceptDrop(DnDConstants.ACTION_COPY);
                    String data = (String) ev.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    if (data == null || data.isBlank()) return;
                    List<Long> isbns = new ArrayList<>();
                    for (String part : data.split(",")) {
                        try { isbns.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) {}
                    }
                    if (!isbns.isEmpty() && onDrop != null) onDrop.accept(shelfId, isbns);
                    ev.dropComplete(true);
                } catch (Exception ex) {
                    ev.dropComplete(false);
                }
            }
        });
    }
}
