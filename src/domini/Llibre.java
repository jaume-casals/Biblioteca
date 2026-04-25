package domini;

public class Llibre {

	private Long ISBN;
	private String Nom;
	private String Autor;
	private Integer Any;
	private String Descripcio;
	private Double Valoracio;
	private Double Preu;
	private Boolean Llegit;
	private String Imatge;
	private byte[] ImatgeBlob;
	private String Notes = "";
	private int Pagines = 0;
	private int PaginesLlegides = 0;
	private boolean hasBlob = false;
	private String Editorial = "";
	private String Serie = "";
	private int Volum = 0;
	private String DataCompra = null;
	private String DataLectura = null;
	private String Idioma = null;
	private String Format = null;
	private boolean Desitjat = false;
	private String PaisOrigen = null;
	private java.util.List<String> Autors = new java.util.ArrayList<>();

	public Llibre(Long isbn, String nom, String autor, Integer any, String descripcio, Double valoracio, Double preu,
			Boolean llegit, String imatge) {
		this.ISBN = isbn;
		this.Nom = nom;
		this.Autor = autor;
		this.Any = any;
		this.Descripcio = descripcio;
		this.Valoracio = valoracio;
		this.Preu = preu;
		this.Llegit = llegit;
		this.Imatge = imatge;
	}

	public Long getISBN() {
		return ISBN;
	}

	public void setISBN(Long iSBN) {
		ISBN = iSBN;
	}

	public String getNom() {
		return Nom;
	}

	public void setNom(String nom) {
		Nom = nom;
	}

	public String getAutor() {
		if (!Autors.isEmpty()) return String.join(", ", Autors);
		return Autor;
	}

	public void setAutor(String autor) {
		Autor = autor;
	}

	public java.util.List<String> getAutors() { return Autors; }
	public void setAutors(java.util.List<String> autors) {
		Autors = autors != null ? new java.util.ArrayList<>(autors) : new java.util.ArrayList<>();
		if (!Autors.isEmpty()) Autor = String.join(", ", Autors);
	}

	public Integer getAny() {
		return Any;
	}

	public void setAny(Integer any) {
		Any = any;
	}

	public String getDescripcio() {
		return Descripcio;
	}

	public void setDescripcio(String descripcio) {
		Descripcio = descripcio;
	}

	public Double getValoracio() {
		return Valoracio;
	}

	public void setValoracio(Double valoracio) {
		Valoracio = valoracio;
	}

	public Double getPreu() {
		return Preu;
	}

	public void setPreu(Double preu) {
		Preu = preu;
	}

	public Boolean getLlegit() {
		return Llegit;
	}

	public void setLlegit(Boolean llegit) {
		Llegit = llegit;
	}

	public String getImatge() {
		return Imatge;
	}

	public void setImatge(String imatge) {
		Imatge = imatge;
	}

	public byte[] getImatgeBlob() { return ImatgeBlob; }
	public void setImatgeBlob(byte[] blob) { ImatgeBlob = blob; }
	public boolean hasBlob() { return hasBlob; }
	public void setHasBlob(boolean v) { hasBlob = v; }

	public String getNotes() { return Notes != null ? Notes : ""; }
	public void setNotes(String notes) { Notes = notes != null ? notes : ""; }
	public int getPagines() { return Pagines; }
	public void setPagines(int p) { Pagines = Math.max(0, p); }
	public int getPaginesLlegides() { return PaginesLlegides; }
	public void setPaginesLlegides(int p) { PaginesLlegides = Math.max(0, p); }
	public String getEditorial() { return Editorial != null ? Editorial : ""; }
	public void setEditorial(String editorial) { Editorial = editorial != null ? editorial : ""; }
	public String getSerie() { return Serie != null ? Serie : ""; }
	public void setSerie(String serie) { Serie = serie != null ? serie : ""; }
	public int getVolum() { return Volum; }
	public void setVolum(int volum) { Volum = Math.max(0, volum); }
	public String getDataCompra() { return DataCompra; }
	public void setDataCompra(String d) { DataCompra = (d != null && !d.trim().isEmpty()) ? d.trim() : null; }
	public String getDataLectura() { return DataLectura; }
	public void setDataLectura(String d) { DataLectura = (d != null && !d.trim().isEmpty()) ? d.trim() : null; }
	public String getIdioma() { return Idioma; }
	public void setIdioma(String i) { Idioma = (i != null && !i.trim().isEmpty()) ? i.trim() : null; }
	public String getFormat() { return Format; }
	public void setFormat(String f) { Format = (f != null && !f.trim().isEmpty()) ? f.trim() : null; }
	public String getPaisOrigen() { return PaisOrigen; }
	public void setPaisOrigen(String p) { PaisOrigen = (p != null && !p.trim().isEmpty()) ? p.trim() : null; }
	public boolean getDesitjat() { return Desitjat; }
	public void setDesitjat(boolean d) { Desitjat = d; }

	@Override
	public String toString() {
		return this.ISBN + " " + this.Nom + " " + this.Autor + " " + this.Any + " " + this.Descripcio + " "
				+ this.Valoracio + " " + this.Preu + " " + this.Llegit + " " + this.Imatge;
	}
}
