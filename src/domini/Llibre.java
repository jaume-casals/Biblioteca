package domini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import persistencia.dao.LlibreDaoCore;
public class Llibre {

	private Long isbn;
	private String nom;
	private String autor;
	private Integer any;
	private String descripcio;
	private Double valoracio;
	private Double preu;
	private Boolean llegit;
	private String imatge;
	private byte[] imatgeBlob;
	private String notes = "";
	private int pagines = 0;
	private int paginesLlegides = 0;
	private boolean teBlob = false;
	private String editorial = "";
	private String serie = "";
	private int volum = 0;
	private String dataCompra = null;
	private String dataLectura = null;
	private String idioma = null;
	private String format = null;
	private boolean desitjat = false;
	private String paisOrigen = null;
	private String estat = null;
	private int exemplars = 1;
	private String llenguaOriginal = null;
	private List<String> autors = new ArrayList<>();
	private String nomCa = null;
	private String nomEs = null;
	private String nomEn = null;
	private boolean heavyFieldsLoaded = true;

	private Llibre() {}

	public Llibre(Long isbn, String nom, String autor, Integer any, String descripcio, Double valoracio, Double preu,
			Boolean llegit, String imatge) {
		this.isbn = isbn;
		this.nom = nom;
		this.autor = autor;
		this.any = any;
		this.descripcio = descripcio;
		this.valoracio = valoracio;
		this.preu = preu;
		this.llegit = llegit;
		this.imatge = imatge;
	}

	/**
	 * Copia els camps canònics "actualitzables" (tot menys el títol per
	 * idioma, el blob de portada, la llista d'autors, les notes, les pàgines,
	 * etc.) a {@code target}. El paràmetre {@code autor} és el string ja
	 * concatenat provinent del validador / formulari; els consumidors que
	 * disposen de la llista analitzada haurien de cridar
	 * {@link #posarAutors(java.util.List)} a continuació, que SOBREESCRIURÀ
	 * el string concatenat. L'ordre en dos passos (vinculació → posarAutors)
	 * és deliberada: vincularUpdateableFields primer deixa preparat el string
	 * concatenat de manera que {@link #obtenirAutors()} sigui consistent
	 * immediatament; després la llista nova permet al consumidor substituir-
	 * lo pel resultat analitzat sense la lògica de divisió a nivell de camp.
	 */
	public static Llibre vincularUpdateableFields(Llibre target, Long isbn, String nom, String autor,
			Integer any, String descripcio, Double valoracio, Double preu, Boolean llegit, String imatge) {
		target.isbn = isbn;
		target.nom = nom;
		target.autor = autor;
		target.posarAny(any);
		target.descripcio = descripcio;
		target.valoracio = valoracio;
		target.preu = preu;
		target.llegit = llegit;
		target.imatge = imatge;
		return target;
	}

	public static Constructor builder() {
		return new Constructor();
	}

	public static final class Constructor {
		private Long isbn;
		private String nom;
		private String autor;
		private Integer any;
		private String descripcio;
		private Double valoracio;
		private Double preu;
		private Boolean llegit;
		private String imatge;

		private Constructor() {}

		public Constructor isbn(Long v) { this.isbn = v; return this; }
		public Constructor nom(String v) { this.nom = v; return this; }
		public Constructor autor(String v) { this.autor = v; return this; }
		public Constructor any(Integer v) { this.any = v; return this; }
		public Constructor descripcio(String v) { this.descripcio = v; return this; }
		public Constructor valoracio(Double v) { this.valoracio = v; return this; }
		public Constructor preu(Double v) { this.preu = v; return this; }
		public Constructor llegit(Boolean v) { this.llegit = v; return this; }
		public Constructor imatge(String v) { this.imatge = v; return this; }

		public Llibre build() {
			return Llibre.vincularUpdateableFields(new Llibre(), isbn, nom, autor, any, descripcio,
				valoracio, preu, llegit, imatge);
		}
	}

	public Long obtenirISBN() {
		return isbn;
	}

	public void posarISBN(Long isbn) {
		this.isbn = isbn;
	}

	public String obtenirNom() {
		return nom;
	}

	public void posarNom(String nom) {
		this.nom = nom;
	}

	public String obtenirNomCa() { return nomCa; }
	public void posarNomCa(String v) { this.nomCa = nullIfBlankTrim(v); }
	public String obtenirNomEs() { return nomEs; }
	public void posarNomEs(String v) { this.nomEs = nullIfBlankTrim(v); }
	public String obtenirNomEn() { return nomEn; }
	public void posarNomEn(String v) { this.nomEn = nullIfBlankTrim(v); }

	/** Retorna {@code s.trim()} si {@code s} no és null ni blanc; altrament {@code null}. */
	private static String nullIfBlankTrim(String s) {
		return (s != null && !s.isBlank()) ? s.trim() : null;
	}

	/** Retorna el títol per al codi d'idioma donat ("ca","es","en"), caient al nom si no hi és. */
	public String obtenirDisplayNom(String lang) {
		String alt = null;
		if ("ca".equals(lang)) alt = nomCa;
		else if ("es".equals(lang)) alt = nomEs;
		else if ("en".equals(lang)) alt = nomEn;
		return (alt != null && !alt.isBlank()) ? alt : nom;
	}

	public String obtenirAutor() { return autor != null ? autor : ""; }

	public List<String> obtenirAutors() { return Collections.unmodifiableList(autors); }

	/** Usat en carregar múltiples files d'autors des de SQL (veure {@link persistencia.dao.LlibreDaoCore#obtenirAll}). */
	public void afegirAutorNom(String nom) {
		if (nom == null || nom.isBlank()) return;
		autors.add(nom);
		autor = String.join(", ", autors);
	}

	public void posarAutors(List<String> autors) {
		this.autors = autors != null ? new ArrayList<>(autors) : new ArrayList<>();
		this.autor = this.autors.isEmpty() ? "" : String.join(", ", this.autors);
	}

	public Integer obtenirAny() {
		return any;
	}

	public void posarAny(Integer any) {
		if (any != null && any < 0)
			throw new BibliotecaException.Validacio(herramienta.i18n.I18n.t("val_any_negatiu", any));
		this.any = any;
	}

	public String obtenirDescripcio() {
		return descripcio;
	}

	public void posarDescripcio(String descripcio) {
		this.descripcio = descripcio != null ? descripcio : "";
	}

	public Double obtenirValoracio() {
		return valoracio;
	}

	public void posarValoracio(Double valoracio) {
		this.valoracio = valoracio;
	}

	public Double obtenirPreu() {
		return preu;
	}

	public void posarPreu(Double preu) {
		this.preu = preu;
	}

	public Boolean obtenirLlegit() {
		return llegit;
	}

	public void posarLlegit(Boolean llegit) {
		this.llegit = llegit;
	}

	public String obtenirImatge() {
		return imatge;
	}

	public void posarImatge(String imatge) {
		this.imatge = imatge;
	}

	public byte[] obtenirImatgeBlob() { return imatgeBlob; }
	public void posarImatgeBlob(byte[] blob) { imatgeBlob = blob; }
	public boolean teBlob() { return teBlob; }
	public void posarHasBlob(boolean teBlob) { this.teBlob = teBlob; }

	public String obtenirNotes() { return notes; }
	public void posarNotes(String notes) { this.notes = notes != null ? notes : ""; }
	public int obtenirPagines() { return pagines; }
	public void posarPagines(int pagines) { this.pagines = Math.max(0, pagines); }
	public int obtenirPaginesLlegides() { return paginesLlegides; }
	public void posarPaginesLlegides(int paginesLlegides) { this.paginesLlegides = Math.max(0, paginesLlegides); }
	public String obtenirEditorial() { return editorial; }
	public void posarEditorial(String editorial) { this.editorial = editorial != null ? editorial : ""; }
	public String obtenirSerie() { return serie; }
	public void posarSerie(String serie) { this.serie = serie != null ? serie : ""; }
	public int obtenirVolum() { return volum; }
	public void posarVolum(int volum) { this.volum = Math.max(0, volum); }
	public String obtenirDataCompra() { return dataCompra; }
	public void posarDataCompra(String dataCompra) { this.dataCompra = nullIfBlankTrim(dataCompra); }
	public String obtenirDataLectura() { return dataLectura; }
	public void posarDataLectura(String dataLectura) { this.dataLectura = nullIfBlankTrim(dataLectura); }
	public String obtenirIdioma() { return idioma; }
	public void posarIdioma(String idioma) { this.idioma = nullIfBlankTrim(idioma); }
	public String obtenirFormat() { return format; }
	public void posarFormat(String format) { this.format = nullIfBlankTrim(format); }
	public String obtenirPaisOrigen() { return paisOrigen; }
	public void posarPaisOrigen(String paisOrigen) { this.paisOrigen = nullIfBlankTrim(paisOrigen); }
	public boolean esDesitjat() { return desitjat; }
	public void posarDesitjat(boolean desitjat) { this.desitjat = desitjat; }
	public String obtenirEstat() { return estat; }
	public void posarEstat(String estat) { this.estat = nullIfBlankTrim(estat); }
	public int obtenirExemplars() { return exemplars; }
	public void posarExemplars(int exemplars) { this.exemplars = Math.max(1, exemplars); }
	public String obtenirLlenguaOriginal() { return llenguaOriginal; }
	public void posarLlenguaOriginal(String llengua) { this.llenguaOriginal = nullIfBlankTrim(llengua); }

	public static Llibre copyOf(Llibre src) {
		Llibre c = Llibre.vincularUpdateableFields(new Llibre(), src.isbn, src.nom, src.autor, src.any, src.descripcio,
			src.valoracio, src.preu, src.llegit, src.imatge);
		c.notes = src.notes; c.pagines = src.pagines; c.paginesLlegides = src.paginesLlegides;
		c.editorial = src.editorial; c.serie = src.serie; c.volum = src.volum;
		c.dataCompra = src.dataCompra; c.dataLectura = src.dataLectura;
		c.idioma = src.idioma; c.format = src.format; c.paisOrigen = src.paisOrigen;
		c.desitjat = src.desitjat; c.estat = src.estat;
		c.exemplars = src.exemplars; c.llenguaOriginal = src.llenguaOriginal;
		c.autors = src.autors != null ? new java.util.ArrayList<>(src.autors) : new java.util.ArrayList<>();
		c.nomCa = src.nomCa; c.nomEs = src.nomEs; c.nomEn = src.nomEn;
		c.teBlob = src.teBlob;
		c.imatgeBlob = src.imatgeBlob != null ? src.imatgeBlob.clone() : null;
		c.heavyFieldsLoaded = src.heavyFieldsLoaded;
		return c;
	}

	public boolean teCampsPesatsCarregats() { return heavyFieldsLoaded; }
	public void posarCampsPesatsCarregats(boolean heavyFieldsLoaded) { this.heavyFieldsLoaded = heavyFieldsLoaded; }

	@Override
	public String toString() {
		return "Llibre{isbn=" + isbn + ", nom=" + nom + ", autor=" + obtenirAutor()
			+ ", any=" + any
			+ ", valoracio=" + valoracio + ", preu=" + preu + ", llegit=" + llegit
			+ ", pagines=" + pagines + ", paginesLlegides=" + paginesLlegides
			+ ", editorial=" + editorial + ", serie=" + serie + ", volum=" + volum
			+ ", idioma=" + idioma + ", format=" + format + ", estat=" + estat
			+ ", exemplars=" + exemplars + ", desitjat=" + desitjat
			+ ", nomCa=" + nomCa + ", nomEs=" + nomEs + ", nomEn=" + nomEn
			+ ", hasBlob=" + (imatgeBlob != null) + "}";
	}
}
