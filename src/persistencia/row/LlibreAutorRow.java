package persistencia.row;

import persistencia.dao.AutorDao;
/** Fila d'unió {@code llibre_autor}; usada per {@link AutorDao} i l'exportació de còpia de seguretat. */
public record LlibreAutorRow(long isbn, int autorId) implements RelationRow {}