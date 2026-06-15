package herramienta.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

/**
 * Streaming RFC-4180 CSV reader. Replaces whole-file loading in {@link CsvUtils#parseLine}
 * for large imports. Returns one row at a time as a {@code String[]}.
 *
 * <p>Contract: {@link #hasNext()} is the only probe; {@link #next()} is
 * guaranteed to return non-null when {@code hasNext()} is true (and throw
 * {@link NoSuchElementException} otherwise). The previous API returned
 * {@code null} for a trailing empty input, forcing callers to null-check
 * {@code next()}; per the tot.txt LOW finding, the contract is now
 * "hasNext is the only probe".
 *
 * <p>Caller-supplied {@link Reader} must be {@code BufferedReader} for line-buffered reads.
 */
public final class Rfc4180Reader implements AutoCloseable {

    private final BufferedReader in;
    private String pending;

    public Rfc4180Reader(Reader r) {
        this.in = r instanceof BufferedReader br ? br : new BufferedReader(r);
    }

    /** Returns true if there is another row to read. The next call to
     *  {@link #next()} will return a non-null {@code String[]}. */
    public boolean hasNext() throws IOException {
        if (pending != null) return true;
        pending = readLogicalRow();
        return pending != null;
    }

    /** Reads the next logical row, joining continuation lines inside quotes. */
    public String[] next() throws IOException {
        if (pending == null) pending = readLogicalRow();
        if (pending == null) throw new NoSuchElementException();
        String[] row = CsvUtils.parseLine(pending);
        pending = null;
        return row;
    }

    /** Reads one logical row (or null at EOF) into a flat string. */
    private String readLogicalRow() throws IOException {
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
        // EOF: discard a trailing empty accumulator (file ended with a newline);
        // return whatever was still being accumulated, or null for a clean EOF.
        return accum.length() == 0 ? null : accum.toString();
    }

    @Override public void close() throws IOException { in.close(); }
}
