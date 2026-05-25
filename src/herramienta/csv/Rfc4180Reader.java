package herramienta.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Streaming RFC-4180 CSV reader. Replaces whole-file loading in {@link CsvUtils#parseLine}
 * for large imports. Returns one row at a time as a {@code String[]}.
 *
 * <p>Caller-supplied {@link Reader} must be {@code BufferedReader} for line-buffered reads.
 */
public final class Rfc4180Reader implements AutoCloseable {

    private final BufferedReader in;
    private boolean eof = false;

    public Rfc4180Reader(Reader r) {
        this.in = r instanceof BufferedReader br ? br : new BufferedReader(r);
    }

    public boolean hasNext() { return !eof; }

    /** Reads the next logical row, joining continuation lines inside quotes. */
    public String[] next() throws IOException {
        if (eof) throw new NoSuchElementException();
        StringBuilder accum = new StringBuilder();
        boolean inQuote = false;
        String line;
        while ((line = in.readLine()) != null) {
            if (accum.length() > 0) accum.append('\n');
            accum.append(line);
            for (int i = 0; i < line.length(); i++) if (line.charAt(i) == '"') inQuote = !inQuote;
            if (!inQuote) return CsvUtils.parseLine(accum.toString());
        }
        eof = true;
        if (accum.length() == 0) return null;
        return CsvUtils.parseLine(accum.toString());
    }

    @Override public void close() throws IOException { in.close(); }
}
