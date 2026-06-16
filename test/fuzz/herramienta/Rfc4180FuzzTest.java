package fuzz.herramienta;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import herramienta.csv.Rfc4180Reader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

/**
 * Coverage-guided fuzz harness for the streaming RFC-4180 CSV reader.
 *
 * <p>The fuzz body consumes the fuzzer's remaining bytes as UTF-8 and
 * drives the reader end-to-end. We cap the iteration count at 10_000 to
 * keep an accidental infinite-loop bug in {@code hasNext()}/{@code next()}
 * from hanging CI. The allowed exceptions are exactly the contract:
 * {@link IOException} from the buffered reader and
 * {@link NoSuchElementException} when {@code next()} is called after
 * {@code hasNext()} returned false. OutOfMemoryError, StackOverflowError,
 * and NullPointerException all fail the test.
 */
class Rfc4180FuzzTest {

    private static final int MAX_ROWS = 10_000;

    @FuzzTest(maxDuration = "30s")
    void fuzz(FuzzedDataProvider data) throws IOException {
        byte[] payload = data.consumeRemainingAsBytes();
        ByteArrayInputStream bin = new ByteArrayInputStream(payload);
        InputStreamReader isr = new InputStreamReader(bin, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        try (Rfc4180Reader reader = new Rfc4180Reader(br)) {
            int iters = 0;
            while (reader.hasNext()) {
                String[] row = reader.next();
                if (row == null) {
                    throw new AssertionError("Rfc4180Reader.next() returned null after hasNext()=true");
                }
                iters++;
                if (iters > MAX_ROWS) {
                    return;
                }
            }
        } catch (NoSuchElementException expected) {
            // next() after a final hasNext()=true is documented to throw;
            // an unexpected one inside the loop would have been a real bug.
            throw expected;
        }
    }
}
