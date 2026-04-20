package domini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import herramienta.FiltreUtils;
import persistencia.ControladorPersistencia;

public class ControladorDomini {
	private static ControladorDomini inst;
	private ControladorPersistencia cp;
	private ArrayList<Llibre> bib;
	private ArrayList<Llista> llistes;

	private static final Comparator<Llibre> compararISBN =
		(a, b) -> a.getISBN().compareTo(b.getISBN());

	public static ControladorDomini getInstance() {
		if (ControladorDomini.inst == null)
			ControladorDomini.inst = new ControladorDomini();
		return ControladorDomini.inst;
	}

	public static void resetForTest() { inst = null; }

	private ControladorDomini() {
		cp = ControladorPersistencia.getInstance();
		bib = new ArrayList<Llibre>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
		if (!"true".equals(System.getProperty("biblioteca.test"))) {
			Thread t = new Thread(this::autoBackup);
			t.setDaemon(true);
			t.start();
		}
	}

	public ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Long ISBN,
			Integer iniciAny, Integer fiAny,
			Double valoracioMin, Double valoracioMax,
			Double preuMin, Double preuMax, Boolean llegit) {
		ArrayList<Llibre> resultat = new ArrayList<Llibre>();
		for (Llibre l : bib) {
			if ((nomAutor == null || FiltreUtils.matchString(nomAutor, l.getAutor()))
					&& (nomLlibre == null || FiltreUtils.matchString(nomLlibre, l.getNom()))
					&& (ISBN == null || FiltreUtils.matchISBN(ISBN, l.getISBN()))
					&& (iniciAny == null || l.getAny() >= iniciAny)
					&& (fiAny == null || l.getAny() <= fiAny)
					&& (valoracioMin == null || l.getValoracio() >= valoracioMin)
					&& (valoracioMax == null || l.getValoracio() <= valoracioMax)
					&& (preuMin == null || l.getPreu() >= preuMin)
					&& (preuMax == null || l.getPreu() <= preuMax)
					&& (llegit == null || l.getLlegit().equals(llegit))) {
				resultat.add(l);
			}
		}
		return resultat;
	}

	public ArrayList<Llibre> getAllLlibres() {
		return bib;
	}

	public ArrayList<Llibre> get10Llibres() {
		return new ArrayList<>(bib.subList(0, Math.min(10, bib.size())));
	}

	public ArrayList<Llibre> get100Llibres(int index) { // Comença des del 0 fins al final
		return new ArrayList<>(bib.subList(100 * index, Math.min(100 * index + 100, bib.size())));
	}

	public int maxIndex100Llibres() { // maxim index que li pots indicar per agafar 100 llibres
		return bib.size() / 100;
	}

	public int getSize() {
		return bib.size();
	}

	public void addLlibre(Llibre l) throws Exception {
		cp.afegirLlibre(l);
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos >= 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " ja existeix a la base de dades");

		pos = -(pos + 1);
		bib.add(pos, l);
	}

	public void deleteLlibre(Llibre l) throws Exception {
		cp.eliminarLlibre(l);

		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public void deleteLlibre(Long ISBN) throws Exception {
		cp.eliminarLlibre(ISBN);

		int pos = Collections.binarySearch(bib,
				new Llibre(ISBN, "", "autor", 13, "descripcio", 1.0, 3.0, false, ""), compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public Llibre getLlibre(long ISBN) throws Exception {

		int index = Collections.binarySearch(bib,
				new Llibre(ISBN, "", "autor", 13, "descripcio", 1.0, 3.0, false, ""), compararISBN);

		if (index < 0)
			throw new Exception("No existeix el llibre amb ISBN " + ISBN);

		return bib.get(index);
	}

	private void autoBackup() {
		if (bib.isEmpty()) return;
		try {
			java.io.File dir = new java.io.File(
				System.getProperty("user.home") + "/.biblioteca/backups");
			dir.mkdirs();
			String ts = java.time.LocalDateTime.now()
				.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
			backupToSQL(new java.io.File(dir, "biblioteca_" + ts + ".sql"));
			java.io.File[] backups = dir.listFiles(
				(d, n) -> n.startsWith("biblioteca_") && n.endsWith(".sql"));
			if (backups != null && backups.length > 5) {
				java.util.Arrays.sort(backups);
				for (int i = 0; i < backups.length - 5; i++) backups[i].delete();
			}
		} catch (Exception ignored) {}
	}

	public void backupToSQL(java.io.File file) throws Exception {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
			pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
			pw.println("DELETE FROM llibre_llista;");
			pw.println("DELETE FROM llista;");
			pw.println("DELETE FROM llibre;");
			for (Llibre l : bib) {
				pw.printf(
					"INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`notes`) VALUES (%d,'%s','%s',%d,'%s',%.4f,%.4f,%b,'%s','%s');%n",
					l.getISBN(),
					sqlEsc(l.getNom()),
					sqlEsc(l.getAutor() != null ? l.getAutor() : ""),
					l.getAny() != null ? l.getAny() : 0,
					sqlEsc(l.getDescripcio() != null ? l.getDescripcio() : ""),
					l.getValoracio() != null ? l.getValoracio() : 0.0,
					l.getPreu() != null ? l.getPreu() : 0.0,
					Boolean.TRUE.equals(l.getLlegit()),
					sqlEsc(l.getImatge() != null ? l.getImatge() : ""),
					sqlEsc(l.getNotes()));
			}
			for (Llista ll : llistes) {
				pw.printf("INSERT INTO llista (`id`,`nom`) VALUES (%d,'%s');%n",
					ll.getId(), sqlEsc(ll.getNom()));
			}
			for (Object[] row : cp.getAllLlibreLlista()) {
				pw.printf("INSERT INTO llibre_llista (`isbn`,`llista_id`,`valoracio`,`llegit`) VALUES (%d,%d,%.4f,%b);%n",
					(Long) row[0], (Integer) row[1], (Double) row[2], (Boolean) row[3]);
			}
		}
	}

	public void restoreFromSQL(java.io.File file) throws Exception {
		cp.executeSQLFile(file);
		bib = new ArrayList<>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
	}

	private static String sqlEsc(String s) {
		return s == null ? "" : s.replace("'", "''");
	}

	// ── Llista (shelf) management ──────────────────────────────────────────────

	public ArrayList<Llista> getAllLlistes() { return llistes; }

	public Llista addLlista(String nom) throws Exception {
		int id = cp.createLlista(nom);
		Llista l = new Llista(id, nom);
		llistes.add(l);
		return l;
	}

	public void deleteLlista(Llista llista) throws Exception {
		cp.deleteLlista(llista.getId());
		llistes.remove(llista);
	}

	public int getCountInLlista(int llistaId) { return cp.getCountInLlista(llistaId); }

	public ArrayList<Llibre> getLlibresInLlista(int llistaId) {
		return cp.getLlibresInLlista(llistaId);
	}

	public ArrayList<Llista> getLlistesForLlibre(long isbn) {
		return cp.getLlistesForLlibre(isbn);
	}

	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception {
		cp.addLlibreToLlista(isbn, llistaId, valoracio, llegit);
	}

	public void removeLlibreFromLlista(long isbn, int llistaId) throws Exception {
		cp.removeLlibreFromLlista(isbn, llistaId);
	}

	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception {
		cp.updateLlibreInLlista(isbn, llistaId, valoracio, llegit);
	}

	public void moveLlistaUp(int id) throws Exception {
		int idx = indexOfLlista(id);
		if (idx > 0) swapLlistesOrdre(idx, idx - 1);
	}

	public void moveLlistaDown(int id) throws Exception {
		int idx = indexOfLlista(id);
		if (idx >= 0 && idx < llistes.size() - 1) swapLlistesOrdre(idx, idx + 1);
	}

	private int indexOfLlista(int id) {
		for (int i = 0; i < llistes.size(); i++)
			if (llistes.get(i).getId() == id) return i;
		return -1;
	}

	private void swapLlistesOrdre(int i, int j) throws Exception {
		for (int k = 0; k < llistes.size(); k++) {
			llistes.get(k).setOrdre(k);
			cp.updateLlistaOrdre(llistes.get(k).getId(), k);
		}
		Llista a = llistes.get(i);
		Llista b = llistes.get(j);
		a.setOrdre(j);
		b.setOrdre(i);
		cp.updateLlistaOrdre(a.getId(), j);
		cp.updateLlistaOrdre(b.getId(), i);
		Collections.swap(llistes, i, j);
	}

	public ArrayList<Llibre> getRecentlyAdded() {
		return cp.getRecentlyAdded(20);
	}

	public void setLlistaColor(int id, String color) throws Exception {
		cp.updateLlistaColor(id, color);
		for (Llista l : llistes) {
			if (l.getId() == id) { l.setColor(color); break; }
		}
	}

	public void prestarLlibre(long isbn, String nom) throws Exception {
		cp.addPrestec(isbn, nom);
	}

	public void retornarLlibre(long isbn) throws Exception {
		cp.returnPrestec(isbn);
	}

	public java.util.Set<Long> getLoanedISBNs() {
		return cp.getLoanedISBNs();
	}
}
