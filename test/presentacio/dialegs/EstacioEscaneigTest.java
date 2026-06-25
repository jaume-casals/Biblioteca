package presentacio.dialegs;

import com.sun.net.httpserver.HttpServer;
import domini.ControladorDomini;
import domini.Llibre;
import herramienta.api.ClientOpenLibrary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;
import presentacio.controladors.ControladorEstacioEscaneig;
import presentacio.listener.EnActualitzarBBDD;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstacioEscaneigTest {

	static {
		System.setProperty("biblioteca.test", "true");
		System.setProperty("biblioteca.h2.url",
			"jdbc:h2:mem:estacio_escaneig;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
	}

	private HttpServer server;
	private boolean returnServerError;
	private long handlerDelayMs;
	private ControladorEstacioEscaneig controller;
	private ControladorDomini cd;
	private JFrame parentFrame;
	private EstacioEscaneigDialog dialog;

	@BeforeEach
	void setUp() throws IOException {
		ControladorDomini.reinicialitzarForTest();
		ControladorPersistencia.reinicialitzarForTest();
		cd = ControladorDomini.getInstance();
		returnServerError = false;
		handlerDelayMs = 0L;
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		int port = server.getAddress().getPort();
		server.createContext("/api/books", exchange -> {
			if (handlerDelayMs > 0) {
				try { Thread.sleep(handlerDelayMs); }
				catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
			}
			if (returnServerError) {
				exchange.sendResponseHeaders(500, -1);
				exchange.close();
				return;
			}
			String query = exchange.getRequestURI().getQuery();
			String marker = "ISBN:";
			int idx = query != null ? query.indexOf(marker) : -1;
			String isbnKeyValue;
			if (idx < 0) {
				exchange.sendResponseHeaders(404, -1);
				exchange.close();
				return;
			}
			int end = query.indexOf("&", idx);
			isbnKeyValue = end < 0 ? query.substring(idx) : query.substring(idx, end);
			String isbnValue = isbnKeyValue.substring(marker.length());
			String body = "{\"" + isbnKeyValue + "\":"
				+ "{\"title\":\"Book-" + isbnValue + "\","
				+ "\"authors\":[{\"name\":\"Frank Herbert\"}],"
				+ "\"publish_date\":\"1965\"}}";
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		});
		server.start();
		ClientOpenLibrary.testBaseUrl = "http://127.0.0.1:" + port;
		controller = new ControladorEstacioEscaneig(cd, noopListener(), null);
	}

	@AfterEach
	void tearDown() {
		if (dialog != null) { dialog.dispose(); dialog = null; }
		if (parentFrame != null) { parentFrame.dispose(); parentFrame = null; }
		if (controller != null) controller.shutdown();
		ClientOpenLibrary.testBaseUrl = null;
		if (server != null) server.stop(0);
		ControladorDomini.reinicialitzarForTest();
		ControladorPersistencia.reinicialitzarForTest();
	}

	private static EnActualitzarBBDD noopListener() {
		return new EnActualitzarBBDD() {
			@Override public void enActualitzarLlibre(Llibre l, boolean esNew) {}
			@Override public void enEliminarLlibre(Llibre l) {}
		};
	}

	@Test
	@DisplayName("valid ISBN + 200 from OL → added=1, book in DB")
	void validIsbnIsAdded() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		controller.setOnResult(latch::countDown);
		long isbn = 9780306406157L;
		controller.submitIsbn(String.valueOf(isbn));
		assertTrue(latch.await(5, TimeUnit.SECONDS), "scan did not complete");
		assertThat(controller.getCountAdded()).isEqualTo(1L);
		assertThat(controller.getCountDuplicate()).isEqualTo(0L);
		assertThat(controller.getCountError()).isEqualTo(0L);
		assertTrue(cd.existsLlibre(isbn), "book must exist in DB after ADDED");
		Llibre l = cd.obtenirLlibre(isbn);
		assertThat(l.obtenirNom()).isNotEmpty();
	}

	@Test
	@DisplayName("duplicate → added=1, duplicate=1, only one book in DB")
	void duplicateIsDetected() throws Exception {
		long isbn = 9780306406157L;
		CountDownLatch latch1 = new CountDownLatch(1);
		controller.setOnResult(latch1::countDown);
		controller.submitIsbn(String.valueOf(isbn));
		assertTrue(latch1.await(5, TimeUnit.SECONDS), "first scan did not complete");
		CountDownLatch latch2 = new CountDownLatch(1);
		controller.setOnResult(latch2::countDown);
		controller.submitIsbn(String.valueOf(isbn));
		assertTrue(latch2.await(5, TimeUnit.SECONDS), "second scan did not complete");
		assertThat(controller.getCountAdded()).isEqualTo(1L);
		assertThat(controller.getCountDuplicate()).isEqualTo(1L);
		assertThat(controller.getCountError()).isEqualTo(0L);
		assertThat(cd.comptarLlibresDB()).isEqualTo(1);
	}

	@Test
	@DisplayName("invalid ISBN 'abc' → error=1, no book in DB")
	void invalidIsbnIsError() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		controller.setOnResult(latch::countDown);
		controller.submitIsbn("abc");
		assertTrue(latch.await(5, TimeUnit.SECONDS), "scan did not complete");
		assertThat(controller.getCountAdded()).isEqualTo(0L);
		assertThat(controller.getCountDuplicate()).isEqualTo(0L);
		assertThat(controller.getCountError()).isEqualTo(1L);
		assertThat(cd.comptarLlibresDB()).isEqualTo(0);
	}

	@Test
	@DisplayName("network error (500) → error=1, no book in DB")
	void networkError() throws Exception {
		returnServerError = true;
		CountDownLatch latch = new CountDownLatch(1);
		controller.setOnResult(latch::countDown);
		controller.submitIsbn("9780306406157");
		assertTrue(latch.await(5, TimeUnit.SECONDS), "scan did not complete");
		assertThat(controller.getCountAdded()).isEqualTo(0L);
		assertThat(controller.getCountDuplicate()).isEqualTo(0L);
		assertThat(controller.getCountError()).isEqualTo(1L);
		assertThat(cd.comptarLlibresDB()).isEqualTo(0);
	}

	@Test
	@DisplayName("queueing: N back-to-back submits with slow handler → all complete in submission order")
	void queueingProcessesInOrder() throws Exception {
		int n = 3;
		long[] isbns = {9780306406157L, 9780451524935L, 9780061120084L};
		handlerDelayMs = 200L;
		CountDownLatch latch = new CountDownLatch(n);
		List<String> completedTitles = Collections.synchronizedList(new ArrayList<>());
		controller.setOnResult(() -> {
			String status = controller.getStatusText();
			String title = status.startsWith("✓ ") ? status.substring(2) : status;
			completedTitles.add(title);
			latch.countDown();
		});
		for (long isbn : isbns) {
			controller.submitIsbn(String.valueOf(isbn));
		}
		assertTrue(latch.await((long) n * handlerDelayMs + 3000, TimeUnit.MILLISECONDS),
			"all N scans should complete (count=" + latch.getCount() + " remaining)");
		assertThat(controller.getCountAdded()).isEqualTo((long) n);
		assertThat(completedTitles).containsExactly(
			"Book-" + isbns[0], "Book-" + isbns[1], "Book-" + isbns[2]);
		assertThat(cd.comptarLlibresDB()).isEqualTo(n);
	}

	@Test
	@DisplayName("dialog: submit ISBN updates lblStatus and increments added counter")
	void dialogSubmitIsbnUpdatesStatus() throws Exception {
		parentFrame = new JFrame();
		dialog = new EstacioEscaneigDialog(parentFrame, cd, noopListener());
		dialog.obtenirTextFieldISBN().setText("9780306406157");
		dialog.obtenirTextFieldISBN().postActionEvent();

		long deadline = System.currentTimeMillis() + 5000L;
		ControladorEstacioEscaneig c = dialog.obtenirControlador();
		while (c.getCountAdded() < 1 && System.currentTimeMillis() < deadline) {
			Thread.sleep(50L);
		}
		assertThat(c.getCountAdded()).isEqualTo(1L);

		final String[] statusHolder = new String[1];
		SwingUtilities.invokeAndWait(() -> statusHolder[0] = dialog.obtenirLblStatus().getText());
		assertThat(statusHolder[0]).startsWith("✓");
		assertThat(dialog.obtenirTextFieldISBN().getText()).isEmpty();
		assertTrue(cd.existsLlibre(9780306406157L));
	}
}