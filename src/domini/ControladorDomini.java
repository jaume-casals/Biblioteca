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

	Comparator<Llibre> compararISBN = new Comparator<Llibre>() {
		@Override
		public int compare(Llibre arg0, Llibre arg1) {
			return arg0.getISBN().compareTo(arg1.getISBN());
		}
	};

	public static ControladorDomini getInstance() {
		if (ControladorDomini.inst == null)
			ControladorDomini.inst = new ControladorDomini();
		return ControladorDomini.inst;
	}

	private ControladorDomini() {
		cp = ControladorPersistencia.getInstance();
		bib = new ArrayList<Llibre>(cp.getAllLlibres());

		Collections.sort(bib, compararISBN);
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
		return (ArrayList<Llibre>) bib.subList(0, Math.min(9, bib.size()));
	}

	public ArrayList<Llibre> get100Llibres(int index) { // Comença des del 0 fins al final
		return (ArrayList<Llibre>) bib.subList(100 * index, Math.min(100 * index + 100, bib.size()));
	}

	public int maxIndex100Llibres() { // maxim index que li pots indicar per agafar 100 llibres
		return bib.size() / 100;
	}

	public int getSize() {
		return bib.size();
	}

	public void addLlibre(Llibre l) throws Exception {
		cp.afegirLlibre(l);
//		for (int i = 0; i < bib.size(); ++i)
//			System.out.println(bib.get(i).getISBN());

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

	public boolean matchISBN(Long ISBN, Long ISBNLlibre) {
		return FiltreUtils.matchISBN(ISBN, ISBNLlibre);
	}

	public boolean matchString(String s, String x) {
		return FiltreUtils.matchString(s, x);
	}

	public void backupToSQL(java.io.File file) throws Exception {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(
				new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
			pw.println("-- Biblioteca backup " + java.time.LocalDate.now());
			pw.println("DELETE FROM llibre;");
			for (Llibre l : bib) {
				pw.printf(
					"INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`) VALUES (%d,'%s','%s',%d,'%s',%.4f,%.4f,%b,'%s');%n",
					l.getISBN(),
					sqlEsc(l.getNom()),
					sqlEsc(l.getAutor() != null ? l.getAutor() : ""),
					l.getAny() != null ? l.getAny() : 0,
					sqlEsc(l.getDescripcio() != null ? l.getDescripcio() : ""),
					l.getValoracio() != null ? l.getValoracio() : 0.0,
					l.getPreu() != null ? l.getPreu() : 0.0,
					Boolean.TRUE.equals(l.getLlegit()),
					sqlEsc(l.getImatge() != null ? l.getImatge() : ""));
			}
		}
	}

	public void restoreFromSQL(java.io.File file) throws Exception {
		cp.executeSQLFile(file);
		bib = new ArrayList<>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
	}

	private static String sqlEsc(String s) {
		return s == null ? "" : s.replace("'", "''");
	}
}
