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
		return Autor;
	}

	public void setAutor(String autor) {
		Autor = autor;
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

	public String getNotes() { return Notes != null ? Notes : ""; }
	public void setNotes(String notes) { Notes = notes != null ? notes : ""; }
	public int getPagines() { return Pagines; }
	public void setPagines(int p) { Pagines = Math.max(0, p); }
	public int getPaginesLlegides() { return PaginesLlegides; }
	public void setPaginesLlegides(int p) { PaginesLlegides = Math.max(0, p); }

	@Override
	public String toString() {
		return this.ISBN + " " + this.Nom + " " + this.Autor + " " + this.Any + " " + this.Descripcio + " "
				+ this.Valoracio + " " + this.Preu + " " + this.Llegit + " " + this.Imatge;
	}
}
