package domini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

	public ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Integer ISBN, Integer iniciAny,
			Integer fiAny, Boolean llegit) {
		ArrayList<Llibre> resultat = new ArrayList<Llibre>();

		for (Llibre l : bib) {
			if ((nomAutor == null || matchString(nomAutor, l.getAutor()))
					&& (nomLlibre == null || matchString(nomLlibre, l.getNom()))
					&& (ISBN == null || matchISBN(ISBN, l.getISBN())) && (iniciAny == null || l.getAny() >= iniciAny)
					&& (fiAny == null || l.getAny() <= fiAny) && (llegit == null || l.getLlegit().equals(llegit))) {
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

		for (int i = 0; i < bib.size(); ++i)
			System.out.println(bib.get(i).getISBN());

		int pos = Collections.binarySearch(bib, l, compararISBN);
		System.out.println("BinarySearch " + pos);
		if (pos >= 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " ja existeix a la base de dades");

		pos = -pos + 1;
		System.out.println("Inserir " + pos);
		bib.add(pos, l);
		for (int i = 0; i < bib.size(); ++i)
			System.out.println(bib.get(i).getISBN());
	}

	public void deleteLlibre(Llibre l) throws Exception {
		cp.eliminarLlibre(l);

		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public void deleteLlibre(Integer ISBN) throws Exception {
		cp.eliminarLlibre(ISBN);

		int pos = Collections.binarySearch(bib,
				new Llibre(ISBN, "", "autor", 13, "descripcio", 1.0, 3.0, false, "portada"), compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public Llibre getLlibre(int ISBN) throws Exception {

		int index = Collections.binarySearch(bib,
				new Llibre(ISBN, "", "autor", 13, "descripcio", 1.0, 3.0, false, "portada"), compararISBN);

		if (index < 0)
			throw new Exception("No existeix el llibre amb ISBN " + ISBN);

		return bib.get(index);
	}

	public boolean matchISBN(Integer ISBN, Integer ISBNLlibre) {
		int tamISBN = 1;
		if (ISBN != null && ISBN > 9)
			tamISBN = ((int) (Math.log10(ISBN) + 1));

		int tamLlibre = (int) (Math.log10(ISBNLlibre) + 1);

		int tamRetallat = (int) Math.pow((int) 10, tamLlibre - tamISBN);

		return (int) (ISBNLlibre / tamRetallat) == ISBN;
	}

	public boolean matchString(String s, String x) {
		for (int i = 0; i < x.length() - s.length() + 1; ++i) {
			System.out.println(x.substring(i, i + s.length()));
			if (x.substring(i, i + s.length()).equals(s))
				return true;
		}
		return false;
	}
}
