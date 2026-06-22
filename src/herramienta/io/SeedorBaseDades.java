package herramienta.io;

import herramienta.config.Configuracio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sembra la base de dades H2 des d'un empaquetat "snapshot" en el primer
 * arrencada d'una instal·lació nova.
 *
 * <p>L'instal·lador NSIS desplega el snapshot a {@code $INSTDIR\data\biblioteca.mv.db}.
 * Quan l'usuari llança per primer cop el binari, el launcher Launch4j fixa
 * {@code user.dir} al directori on resideix l'EXE ({@code $INSTDIR}), per la
 * qual cosa la ruta del snapshot és sempre
 * {@code <user.dir>/data/biblioteca.mv.db}. Si la base de dades de
 * l'usuari ({@code ~/.biblioteca/biblioteca.mv.db}) encara no existeix,
 * es copia del snapshot al directori de perfil; si ja existeix, no es
 * toca (l'usuari ja té dades pròpies).
 *
 * <p>El camí NO s'executa quan:
 * <ul>
 *   <li>El sistema té la propietat {@code -Dbiblioteca.test=true}
 *       (els JUnit proven sobre una BD independent i no han de sembrar
 *       res al perfil real de l'usuari).</li>
 *   <li>El snapshot no és al CWD (mode desenvolupador: l'app es llança
 *       des de {@code bin/} via {@code make run} i no hi ha cap
 *       {@code data/} al costat).</li>
 *   <li>La BD destí ja existeix.</li>
 * </ul>
 */
public final class SeedorBaseDades {

    private static final Logger LOG = Logger.getLogger(SeedorBaseDades.class.getName());

    private SeedorBaseDades() {}

    /** Nom del subdirectori on l'instal·lador NSIS deixa el snapshot. */
    private static final String DATA_SUBDIR = "data";
    /** Nom del fitxer H2 dins el subdirectori. */
    private static final String DB_FILENAME = "biblioteca.mv.db";

    /**
     * Comprova si cal sembrar i, si és així, copia el snapshot a
     * {@code ~/.biblioteca/}. No llença cap excepció — una fallada
     * de còpia es registra com a WARNING i l'app continua amb la BD
     * buida (l'inicialització normal d'H2 crearà una base de dades
     * nova).
     *
     * @return {@code true} si s'ha sembrat, {@code false} si no calia
     *         o no s'ha pogut.
     */
    public static boolean seedSiCal() {
        if ("true".equals(System.getProperty("biblioteca.test"))) {
            return false;
        }

        Path desti = Configuracio.bibliotecaDir().resolve(DB_FILENAME);
        if (Files.exists(desti)) {
            return false;
        }

        Path origen = snapshotPath();
        if (origen == null || !Files.exists(origen)) {
            return false;
        }

        try {
            Files.createDirectories(desti.getParent());
            Files.copy(origen, desti, StandardCopyOption.REPLACE_EXISTING);
            LOG.log(Level.INFO, "Base de dades sembrada des de {0} a {1} ({2} bytes)",
                new Object[]{origen, desti, Files.size(desti)});
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No s''ha pogut sembrar la base de dades des de " + origen, e);
            return false;
        }
    }

    /**
     * Calcula la ruta esperada del snapshot. Retorna {@code null} si
     * {@code user.dir} no és accessible per algun motiu estrany.
     */
    private static Path snapshotPath() {
        String cwd = System.getProperty("user.dir");
        if (cwd == null || cwd.isEmpty()) return null;
        return Paths.get(cwd, DATA_SUBDIR, DB_FILENAME);
    }
}