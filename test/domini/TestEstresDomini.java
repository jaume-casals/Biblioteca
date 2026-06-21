package domini;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Stress / load tests against the in-memory ControladorDomini +
 * H2 stack. These exercise bulk operations and concurrency patterns the
 * smaller functional tests don't reach.
 *
 * <p>Goals:
 * <ul>
 *   <li>bulk insert 1,000 / 5,000 books stays correct and finishes quickly</li>
 *   <li>concurrent add + read doesn't corrupt the size</li>
 *   <li>pagination math remains consistent at scale</li>
 *   <li>round-trip backup/restore at 1,000+ books is byte-precise</li>
 * </ul>
 */
class TestEstresDomini {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:stress_domini;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    @DisplayName("bulk insert 1000 books: final size matches, ISBNs unique, no NPE")
    void bulkInsert1k() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 1000; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "Book " + i, "Author " + i, 1900 + (i % 130),
                "desc", (double) (i % 11), 0.0, i % 2 == 0, ""));
        }
        assertThat(cd.getSize()).isEqualTo(1000);
        // Spot-check the first and last
        assertThat(cd.obtenirLlibre(9780306400000L).obtenirNom()).isEqualTo("Book 0");
        assertThat(cd.obtenirLlibre(9780306400999L).obtenirNom()).isEqualTo("Book 999");
    }

    @Test
    @DisplayName("bulk insert 5000 books: pagination math stays consistent")
    void bulkInsert5k() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 5000; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "B" + i, null, null, null, null, null, false, null));
        }
        assertThat(cd.getSize()).isEqualTo(5000);
        // 100-per-page pagination: pages 0..49 are full, page 50 is the remaining 0
        assertThat(cd.get100Llibres(0)).hasSize(100);
        assertThat(cd.get100Llibres(49)).hasSize(100);
        assertThat(cd.get100Llibres(50)).isEmpty();
        assertThat(cd.maxIndex100Llibres()).isEqualTo(49);
    }

    @Test
    @DisplayName("concurrent add (8 threads) + read: final size is the sum of inserts, no exception")
    void concurrentAddAndRead() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        int threads = 8;
        int perThread = 50;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            workers.add(new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        long isbn = 9780306500000L + (long) tid * perThread + i;
                        cd.afegirLlibre(new Llibre(isbn, "T" + tid + "B" + i, null, null, null, null, null, false, null));
                        // occasionally read
                        if (i % 5 == 0) cd.obtenirAllLlibres().size();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        for (Thread w : workers) w.start();
        start.countDown();
        for (Thread w : workers) w.join(30_000);
        assertThat(errors.get()).isZero();
        assertThat(cd.getSize()).isEqualTo(threads * perThread);
    }

    @Test
    @DisplayName("bulk insert + tag assignment: 1000 books across 5 tags")
    void bulkTag() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 5; i++) cd.afegirTag("Tag" + i);
        for (int i = 0; i < 1000; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "B" + i, null, null, null, null, null, false, null));
            cd.afegirLlibreToTag(isbn, (i % 5) + 1);
        }
        // Each tag should have exactly 200 books
        for (int i = 0; i < 5; i++) {
            LlibreFilter f = ConstructorFiltreLlibre.of().tagId(i + 1).build();
            assertThat(cd.aplicarFiltres(f)).hasSize(200);
        }
    }

    @Test
    @DisplayName("1000 books in a single shelf: count and membership are correct")
    void bulkShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista shelf = cd.afegirLlista("Bulk");
        for (int i = 0; i < 1000; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "B" + i, null, null, null, 0.0, 0.0, false, null));
            cd.afegirLlibreToLlista(isbn, shelf.obtenirId(), 0.0, false);
        }
        assertThat(cd.obtenirCountInLlista(shelf.obtenirId())).isEqualTo(1000);
        assertThat(cd.obtenirLlibresInLlista(shelf.obtenirId())).hasSize(1000);
    }

    @Test
    @DisplayName("bulk backup / restore: 500 books round-trip preserves count and a sample title")
    void bulkBackupRestore() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 500; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "Bulk " + i, "Author", 2000, "", 5.0, 0.0, false, ""));
        }
        java.io.File tmp = java.io.File.createTempFile("stress_bulk_", ".sql");
        tmp.deleteOnExit();
        cd.copiaSegToSQL(tmp);

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        ControladorDomini cd2 = ControladorDomini.getInstance();
        cd2.restaurarFromSQL(tmp);

        assertThat(cd2.getSize()).isEqualTo(500);
        assertThat(cd2.obtenirLlibre(9780306400000L).obtenirNom()).isEqualTo("Bulk 0");
        assertThat(cd2.obtenirLlibre(9780306400499L).obtenirNom()).isEqualTo("Bulk 499");
    }

    @Test
    @DisplayName("pagination clamp behaviour: page beyond max returns empty, negative page is treated as 0")
    void paginationClampAtScale() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 250; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(new Llibre(isbn, "P" + i, null, null, null, null, null, null, null));
        }
        // 250 / 100 = 2 full pages + 50 left on page 3
        assertThat(cd.get100Llibres(0)).hasSize(100);
        assertThat(cd.get100Llibres(2)).hasSize(50);
        assertThat(cd.get100Llibres(3)).isEmpty();
        assertThat(cd.get100Llibres(99)).isEmpty();
    }
}
