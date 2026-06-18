package herramienta.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

/**
 * Lector CSV RFC-4180 en streaming. Substitueix la càrrega de tot el fitxer
 * a {@link UtilitatsCsv#analitzarLine} per a importacions grans. Retorna una
 * fila a la vegada com a {@code String[]}.
 *
 * <p>Contracte: {@link #hasNext()} és l'única sonda; {@link #next()} està
 * garantit que retorna no-null quan {@code hasNext()} és cert (i llança
 * {@link NoSuchElementException} altrament). L'API anterior retornava
 * {@code null} per a una entrada buida final, forçant els consumidors a
 * comprovar null a {@code next()}; segons el finding LOW de tot.txt, el
 * contracte ara és "hasNext és l'única sonda".
 *
 * <p>El {@link Reader} subministrat pel consumidor ha de ser {@code BufferedReader}
 * per a lectures línia a línia amb buffer.
 */
public final class Rfc4180Reader implements AutoCloseable {

    private final BufferedReader in;
    private String pending;

    public Rfc4180Reader(Reader r) {
        this.in = r instanceof BufferedReader br ? br : new BufferedReader(r);
    }

    /** Retorna cert si hi ha una altra fila per llegir. La propera crida a
     *  {@link #next()} retornarà un {@code String[]} no-null. */
    public boolean hasNext() throws IOException {
        if (pending != null) return true;
        pending = llegirLogicalRow();
        return pending != null;
    }

    /** Llegeix la següent fila lògica, unint línies de continuació dins de cometes. */
    public String[] next() throws IOException {
        if (pending == null) pending = llegirLogicalRow();
        if (pending == null) throw new NoSuchElementException();
        String[] row = UtilitatsCsv.analitzarLine(pending);
        pending = null;
        return row;
    }

    /** Llegeix una fila lògica (o null a EOF) en una cadena plana. */
    private String llegirLogicalRow() throws IOException {
        StringBuilder accum = new StringBuilder();
        boolean inQuote = false;
        String line;
        while ((line = in.readLine()) != null) {
            if (accum.length() > 0) accum.append('\n');
            accum.append(line);
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c != '"') continue;
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            }
            if (!inQuote) return accum.toString();
        }
        // EOF: descarta un acumulador buit final (el fitxer ha acabat
        // amb un newline); retorna el que s'estava acumulant, o null
        // per a un EOF net.
        return accum.length() == 0 ? null : accum.toString();
    }

    @Override public void close() throws IOException { in.close(); }
}
