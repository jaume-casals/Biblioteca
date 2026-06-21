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
 * <p>Contracte: {@link #hasNext()} reflexa un flag {@code eof} intern que
 * només es posa a {@code true} quan {@link #next()} consumeix l'última fila
 * disponible. Inicialment — inclús amb entrada buida — {@code hasNext()}
 * retorna {@code true}. La primera crida a {@code next()} que troba EOF
 * retorna {@code null} i posa {@code eof=true}; les crides següents a
 * {@code next()} (inclosa una segona després d'un {@code null}) llancen
 * {@link NoSuchElementException}.
 *
 * <p>El {@link Reader} subministrat pel consumidor ha de ser {@code BufferedReader}
 * per a lectures línia a línia amb buffer.
 */
public final class Rfc4180Reader implements AutoCloseable {

    private final BufferedReader in;
    private String pending;
    private boolean eof;

    public Rfc4180Reader(Reader r) {
        this.in = r instanceof BufferedReader br ? br : new BufferedReader(r);
    }

    /** Retorna cert mentre no s'hagi exhaurit el flux. */
    public boolean hasNext() throws IOException {
        return !eof;
    }

    /** Llegeix la següent fila lògica, unint línies de continuació dins de cometes.
     *  Retorna {@code null} a la primera crida que troba EOF; les crides següents
     *  llencen {@link NoSuchElementException}. */
    public String[] next() throws IOException {
        if (eof) throw new NoSuchElementException();
        if (pending == null) pending = llegirLogicalRow();
        if (pending == null) {
            eof = true;
            return null;
        }
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
