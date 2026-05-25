package persistencia;

import java.sql.Connection;

/**
 * Accessor-style facade over the package-private DAOs in {@code persistencia}.
 * Use as: {@code Persistencia p = new Persistencia(conn); p.llibreDao().getAll();}
 *
 * <p>DAOs are cached per instance so that cross-DAO references (e.g. {@link TagDao}
 * needing {@link LlibreDao}) always see the same underlying objects.
 * {@link ControladorPersistencia} constructs a single {@code Persistencia} and
 * delegates all DAO access through it.
 */
public final class Persistencia {
    private final Connection con;
    private final LlibreDao llibreDao;
    private final LlistaDao llistaDao;
    private final TagDao tagDao;
    private final PrestecDao prestecDao;
    private final AutorDao autorDao;

    public Persistencia(Connection con) {
        this.con = con;
        this.llibreDao = new LlibreDao(con);
        this.llistaDao = new LlistaDao(con);
        this.tagDao = new TagDao(con);
        this.prestecDao = new PrestecDao(con);
        this.autorDao = new AutorDao(con);
    }

    public Connection connection()   { return con; }
    public LlibreDao  llibreDao()     { return llibreDao; }
    public LlistaDao  llistaDao()     { return llistaDao; }
    public TagDao      tagDao()        { return tagDao; }
    public PrestecDao prestecDao()     { return prestecDao; }
    public AutorDao    autorDao()      { return autorDao; }
}