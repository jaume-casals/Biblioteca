package domini;

import java.util.ArrayList;

enum ordre { //Maneres possibles d'ordenar els llibres
    Nom,
    Autor,
    Any,
    Valoracio,
    Llegit
}

public class Biblioteca extends ArrayList<Llibre> {

    private boolean inverse = false; //Indica si la ordenació és inversa
    private ordre ordenacio; //Com està ordenat actualment
    
    Biblioteca() {
        super();

    }

    Biblioteca(ArrayList<Llibre> bib) {
        super(bib);
    }

}
