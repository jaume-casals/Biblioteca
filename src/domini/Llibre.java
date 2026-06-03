package domini;

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
	private boolean hasBlob = false;
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
	private java.util.List<String> autors = new java.util.ArrayList<>();
	private String nomCa = null;
	private String nomEs = null;
	private String nomEn = null;
	private boolean heavyFieldsLoaded = true;

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

	public Long getISBN() {
		return isbn;
	}

	public void setISBN(Long isbn) {
		this.isbn = isbn;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getNomCa() { return nomCa; }
	public void setNomCa(String v) { this.nomCa = (v != null && !v.isBlank()) ? v.trim() : null; }
	public String getNomEs() { return nomEs; }
	public void setNomEs(String v) { this.nomEs = (v != null && !v.isBlank()) ? v.trim() : null; }
	public String getNomEn() { return nomEn; }
	public void setNomEn(String v) { this.nomEn = (v != null && !v.isBlank()) ? v.trim() : null; }

	/** Returns the title for the given lang code ("ca","es","en"), falling back to nom. */
	public String getDisplayNom(String lang) {
		String alt = null;
		if ("ca".equals(lang)) alt = nomCa;
		else if ("es".equals(lang)) alt = nomEs;
		else if ("en".equals(lang)) alt = nomEn;
		return (alt != null && !alt.isBlank()) ? alt : nom;
	}

	public String getAutor() {
		if (!autors.isEmpty()) return String.join(", ", autors);
		return autor != null ? autor : "";
	}

	public void setAutor(String autor) {
		this.autor = autor;
		this.autors = (autor != null && !autor.isBlank())
			? new java.util.ArrayList<>(java.util.List.of(autor))
			: new java.util.ArrayList<>();
	}

	public java.util.List<String> getAutors() { return new java.util.ArrayList<>(autors); }
	public void setAutors(java.util.List<String> autors) {
		this.autors = autors != null ? new java.util.ArrayList<>(autors) : new java.util.ArrayList<>();
		this.autor = this.autors.isEmpty() ? this.autor : String.join(", ", this.autors);
	}

	public Integer getAny() {
		return any;
	}

	public void setAny(Integer any) {
		if (any != null && any < 0)
			throw new BibliotecaException.Validation(herramienta.I18n.t("val_any_negatiu", any));
		this.any = any;
	}

	public String getDescripcio() {
		return descripcio;
	}

	public void setDescripcio(String descripcio) {
		this.descripcio = descripcio;
	}

	public Double getValoracio() {
		return valoracio;
	}

	public void setValoracio(Double valoracio) {
		this.valoracio = valoracio;
	}

	public Double getPreu() {
		return preu;
	}

	public void setPreu(Double preu) {
		this.preu = preu;
	}

	public Boolean getLlegit() {
		return llegit;
	}

	public void setLlegit(Boolean llegit) {
		this.llegit = llegit;
	}

	public String getImatge() {
		return imatge;
	}

	public void setImatge(String imatge) {
		this.imatge = imatge;
	}

	public byte[] getImatgeBlob() { return imatgeBlob; }
	public void setImatgeBlob(byte[] blob) { imatgeBlob = blob; }
	public boolean hasBlob() { return hasBlob; }
	public void setHasBlob(boolean hasBlob) { this.hasBlob = hasBlob; }

	public String getNotes() { return notes != null ? notes : ""; }
	public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
	public int getPagines() { return pagines; }
	public void setPagines(int pagines) { this.pagines = Math.max(0, pagines); }
	public int getPaginesLlegides() { return paginesLlegides; }
	public void setPaginesLlegides(int paginesLlegides) { this.paginesLlegides = Math.max(0, paginesLlegides); }
	public String getEditorial() { return editorial != null ? editorial : ""; }
	public void setEditorial(String editorial) { this.editorial = editorial != null ? editorial : ""; }
	public String getSerie() { return serie != null ? serie : ""; }
	public void setSerie(String serie) { this.serie = serie != null ? serie : ""; }
	public int getVolum() { return volum; }
	public void setVolum(int volum) { this.volum = Math.max(0, volum); }
	public String getDataCompra() { return dataCompra; }
	public void setDataCompra(String dataCompra) { this.dataCompra = (dataCompra != null && !dataCompra.trim().isEmpty()) ? dataCompra.trim() : null; }
	public String getDataLectura() { return dataLectura; }
	public void setDataLectura(String dataLectura) { this.dataLectura = (dataLectura != null && !dataLectura.trim().isEmpty()) ? dataLectura.trim() : null; }
	public String getIdioma() { return idioma; }
	public void setIdioma(String idioma) { this.idioma = (idioma != null && !idioma.trim().isEmpty()) ? idioma.trim() : null; }
	public String getFormat() { return format; }
	public void setFormat(String format) { this.format = (format != null && !format.trim().isEmpty()) ? format.trim() : null; }
	public String getPaisOrigen() { return paisOrigen; }
	public void setPaisOrigen(String paisOrigen) { this.paisOrigen = (paisOrigen != null && !paisOrigen.trim().isEmpty()) ? paisOrigen.trim() : null; }
	public boolean getDesitjat() { return desitjat; }
	public void setDesitjat(boolean desitjat) { this.desitjat = desitjat; }
	public String getEstat() { return estat; }
	public void setEstat(String estat) { this.estat = (estat != null && !estat.trim().isEmpty()) ? estat.trim() : null; }
	public int getExemplars() { return exemplars; }
	public void setExemplars(int exemplars) { this.exemplars = Math.max(1, exemplars); }
	public String getLlenguaOriginal() { return llenguaOriginal; }
	public void setLlenguaOriginal(String llengua) { this.llenguaOriginal = (llengua != null && !llengua.trim().isEmpty()) ? llengua.trim() : null; }

	public static Llibre copyOf(Llibre src) {
		Llibre c = new Llibre(src.isbn, src.nom, src.autor, src.any, src.descripcio,
			src.valoracio, src.preu, src.llegit, src.imatge);
		c.notes = src.notes; c.pagines = src.pagines; c.paginesLlegides = src.paginesLlegides;
		c.editorial = src.editorial; c.serie = src.serie; c.volum = src.volum;
		c.dataCompra = src.dataCompra; c.dataLectura = src.dataLectura;
		c.idioma = src.idioma; c.format = src.format; c.paisOrigen = src.paisOrigen;
		c.desitjat = src.desitjat; c.estat = src.estat;
		c.exemplars = src.exemplars; c.llenguaOriginal = src.llenguaOriginal;
		c.autors = src.autors != null ? new java.util.ArrayList<>(src.autors) : new java.util.ArrayList<>();
		c.nomCa = src.nomCa; c.nomEs = src.nomEs; c.nomEn = src.nomEn;
		c.hasBlob = src.hasBlob;
		c.imatgeBlob = src.imatgeBlob;
		c.heavyFieldsLoaded = src.heavyFieldsLoaded;
		return c;
	}

	public boolean isHeavyFieldsLoaded() { return heavyFieldsLoaded; }
	public void setHeavyFieldsLoaded(boolean heavyFieldsLoaded) { this.heavyFieldsLoaded = heavyFieldsLoaded; }

	@Override
	public String toString() {
		return "Llibre{isbn=" + isbn + ", nom=" + nom + ", autor=" + getAutor()
			+ ", autors=" + autors + ", any=" + any
			+ ", valoracio=" + valoracio + ", preu=" + preu + ", llegit=" + llegit
			+ ", pagines=" + pagines + ", paginesLlegides=" + paginesLlegides
			+ ", editorial=" + editorial + ", serie=" + serie + ", volum=" + volum
			+ ", idioma=" + idioma + ", format=" + format + ", estat=" + estat
			+ ", exemplars=" + exemplars + ", desitjat=" + desitjat
			+ ", nomCa=" + nomCa + ", nomEs=" + nomEs + ", nomEn=" + nomEn
			+ ", hasBlob=" + (imatgeBlob != null) + "}";
	}
}
