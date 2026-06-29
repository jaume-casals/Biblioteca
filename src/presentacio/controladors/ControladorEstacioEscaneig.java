package presentacio.controladors;

import domini.BibliotecaException;
import domini.Llibre;
import herramienta.api.ClientOpenLibrary;
import herramienta.i18n.I18n;
import persistencia.contract.EscritorBiblioteca;
import presentacio.listener.EnActualitzarBBDD;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ControladorEstacioEscaneig {

	public enum Outcome { ADDED, DUPLICATE, INVALID, NETWORK_ERROR, OTHER_ERROR }

	private static final EnumMap<Outcome, Color> OUTCOME_COLORS = new EnumMap<>(Outcome.class);
	static {
		OUTCOME_COLORS.put(Outcome.ADDED, new Color(0, 128, 0));
		OUTCOME_COLORS.put(Outcome.DUPLICATE, new Color(180, 140, 0));
		OUTCOME_COLORS.put(Outcome.INVALID, new Color(170, 0, 0));
		OUTCOME_COLORS.put(Outcome.NETWORK_ERROR, new Color(170, 0, 0));
		OUTCOME_COLORS.put(Outcome.OTHER_ERROR, new Color(170, 0, 0));
	}

	public static final class Result {
		public final Outcome outcome;
		public final String message;
		public final long isbn;
		public final String bookTitle;

		public Result(Outcome outcome, String message, long isbn, String bookTitle) {
			this.outcome = outcome;
			this.message = message;
			this.isbn = isbn;
			this.bookTitle = bookTitle;
		}
	}

	private final EscritorBiblioteca cd;
	private final EnActualitzarBBDD listener;
	private final Runnable onShutdown;

	private final Semaphore sem = new Semaphore(1);
	private final Deque<String> queue = new ArrayDeque<>();
	private volatile boolean shutdown = false;

	private final LongAdder countAdded = new LongAdder();
	private final LongAdder countDuplicate = new LongAdder();
	private final LongAdder countError = new LongAdder();

	private LongAdder counterFor(Outcome o) {
		return switch (o) {
			case ADDED -> countAdded;
			case DUPLICATE -> countDuplicate;
			default -> countError;
		};
	}
	private volatile String statusText;
	private volatile Color statusColor;

	private volatile Runnable onResult;

	public ControladorEstacioEscaneig(EscritorBiblioteca cd, EnActualitzarBBDD listener, Runnable onShutdown) {
		this.cd = cd;
		this.listener = listener;
		this.onShutdown = onShutdown;
		this.statusText = I18n.t("estacio_status_idle");
		this.statusColor = Color.DARK_GRAY;
	}

	public void submitIsbn(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) return;
		if (shutdown) return;
		if (!sem.tryAcquire()) {
			synchronized (queue) { queue.addLast(trimmed); }
			return;
		}
		runWorker(trimmed);
	}

	public void setOnResult(Runnable r) { this.onResult = r; }

	public void shutdown() {
		shutdown = true;
		synchronized (queue) { queue.clear(); }
		if (onShutdown != null) onShutdown.run();
	}

	private void runWorker(String trimmed) {
		SwingWorker<Result, Void> worker = new SwingWorker<>() {
			@Override
			protected Result doInBackground() {
				return processIsbn(trimmed);
			}
			@Override
			protected void done() {
				Result r;
				try { r = get(); }
				catch (Exception e) {
					r = result(Outcome.OTHER_ERROR, 0L, null, "estacio_status_error", e.getMessage());
				}
				finishOneResult(r);
			}
		};
		worker.execute();
	}

	private static Result result(Outcome o, long isbn, String title, String i18nKey, Object... args) {
		return new Result(o, I18n.t(i18nKey, args), isbn, title);
	}

	private Result processIsbn(String trimmed) {
		long isbn;
		try {
			isbn = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return result(Outcome.INVALID, 0L, null, "estacio_status_invalid");
		}
		try {
			if (cd.existsLlibre(isbn)) {
				return result(Outcome.DUPLICATE, isbn, null, "estacio_status_duplicate", trimmed);
			}
		} catch (Exception e) {
			return result(Outcome.OTHER_ERROR, isbn, null, "estacio_status_error", e.getMessage());
		}

		Map<String, String> meta;
		try {
			meta = ClientOpenLibrary.lookupByISBN(trimmed);
		} catch (Exception e) {
			return result(Outcome.NETWORK_ERROR, isbn, null, "estacio_status_network");
		}
		if (meta.containsKey("error")) {
			return result(Outcome.NETWORK_ERROR, isbn, null, "estacio_status_network");
		}

		String title = meta.get("title");
		if (title == null || title.isBlank()) {
			return result(Outcome.OTHER_ERROR, isbn, null, "estacio_status_error", "no title");
		}

		Llibre book = buildBook(isbn, meta);
		try {
			cd.afegirLlibre(book);
		} catch (BibliotecaException.Duplicat dup) {
			return result(Outcome.DUPLICATE, isbn, title, "estacio_status_duplicate", trimmed);
		} catch (Exception e) {
			return result(Outcome.OTHER_ERROR, isbn, title, "estacio_status_error", e.getMessage());
		}

		if (listener != null) {
			SwingUtilities.invokeLater(() -> {
				try { listener.enActualitzarLlibre(book, true); }
				catch (Exception ignored) {}
			});
		}
		return result(Outcome.ADDED, isbn, title, "estacio_status_added", title);
	}

	private static Llibre buildBook(long isbn, Map<String, String> meta) {
		Llibre.Constructor b = Llibre.builder().isbn(isbn);
		String title = meta.get("title");
		if (title != null) b.nom(title);
		String autor = meta.get("autor");
		if (autor != null) b.autor(autor);
		Integer any = parseIntOrNull(meta.get("any"));
		if (any != null) b.any(any);
		String descripcio = meta.get("descripcio");
		if (descripcio != null) b.descripcio(descripcio);
		return b.build();
	}

	private static Integer parseIntOrNull(String s) {
		if (s == null || s.isBlank()) return null;
		try { return Integer.parseInt(s.trim()); }
		catch (NumberFormatException e) { return null; }
	}

	private void finishOneResult(Result r) {
		counterFor(r.outcome).increment();
		statusText = r.message;
		statusColor = OUTCOME_COLORS.getOrDefault(r.outcome, Color.DARK_GRAY);

		Runnable cb = onResult;
		if (cb != null) {
			try { cb.run(); } catch (Exception ignored) {}
		}

		sem.release();
		if (shutdown) {
			synchronized (queue) { queue.clear(); }
			return;
		}
		String next;
		synchronized (queue) { next = queue.pollFirst(); }
		if (next != null) {
			if (!sem.tryAcquire()) {
				synchronized (queue) { queue.addFirst(next); }
				return;
			}
			runWorker(next);
		}
	}

	public String getStatusText() { return statusText; }
	public Color getStatusColor() { return statusColor; }
	public long getCountAdded() { return countAdded.sum(); }
	public long getCountDuplicate() { return countDuplicate.sum(); }
	public long getCountError() { return countError.sum(); }
}