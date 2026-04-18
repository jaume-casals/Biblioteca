import java.util.ArrayList;

import domini.Llibre;
import herramienta.FiltreUtils;
import herramienta.checkLlibre;

public class BibliotecaTest {

    // ── ANSI colours ─────────────────────────────────────────────────────────
    private static final String G = "\u001B[32m";
    private static final String R = "\u001B[31m";
    private static final String C = "\u001B[36m";
    private static final String B = "\u001B[1m";
    private static final String X = "\u001B[0m";

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testLlibre();
        testCountDig();
        testCheackLlibre();
        testMatchISBN();
        testMatchString();
        testFiltreLogic();

        printSummary();
        System.exit(failed > 0 ? 1 : 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    static void section(String name) {
        System.out.println("\n" + C + B + "── " + name + " ──" + X);
    }

    static void pass(String name) {
        passed++;
        System.out.println(G + "  ✓ " + X + name);
    }

    static void fail(String name, String reason) {
        failed++;
        System.out.println(R + B + "  ✗ " + name + X + R + "  →  " + reason + X);
    }

    static void check(String name, boolean ok) {
        if (ok) pass(name); else fail(name, "condition was false");
    }

    static void eq(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) pass(name);
        else fail(name, "expected [" + expected + "] got [" + actual + "]");
    }

    static void isNull(String name, Object obj) {
        if (obj == null) pass(name); else fail(name, "expected null, got " + obj);
    }

    static void notNull(String name, Object obj) {
        if (obj != null) pass(name); else fail(name, "expected non-null, got null");
    }

    static void throws_(String name, Runnable r) {
        try { r.run(); fail(name, "expected exception, none thrown"); }
        catch (Exception e) { pass(name); }
    }

    static void printSummary() {
        System.out.println("\n" + B + "═══════════════════════════════════════" + X);
        int total = passed + failed;
        String color = failed == 0 ? G : R;
        System.out.printf(B + "  %s%d/%d passed%s   %s%d failed%s%n" + X,
            G, passed, total, X, failed > 0 ? R : G, failed, X);
        System.out.println(B + "═══════════════════════════════════════" + X);
    }

    // ── Tests: Llibre ─────────────────────────────────────────────────────────
    static void testLlibre() {
        section("Llibre — constructor & getters");
        Long   isbn  = 97884179104580L;
        String nom   = "Títol de prova";
        String autor = "Autor Prova";
        int    any   = 2024;
        String desc  = "Descripció";
        double val   = 8.5;
        double preu  = 19.99;
        String port  = "portades/test.jpg";

        Llibre l = new Llibre(isbn, nom, autor, any, desc, val, preu, false, port);

        eq("ISBN",       isbn,  l.getISBN());
        eq("Nom",        nom,   l.getNom());
        eq("Autor",      autor, l.getAutor());
        eq("Any",        any,   l.getAny());
        eq("Descripcio", desc,  l.getDescripcio());
        eq("Valoracio",  val,   l.getValoracio());
        eq("Preu",       preu,  l.getPreu());
        eq("Llegit",     false, l.getLlegit());
        eq("Portada",    port,  l.getPortada());

        section("Llibre — setters");
        l.setNom("Nou nom");       eq("setNom",   "Nou nom",   l.getNom());
        l.setAutor("Nou autor");   eq("setAutor", "Nou autor", l.getAutor());
        l.setAny(2025);            eq("setAny",   2025,        l.getAny());
        l.setValoracio(5.0);       eq("setVal",   5.0,         l.getValoracio());
        l.setPreu(9.99);           eq("setPreu",  9.99,        l.getPreu());
        l.setLlegit(true);         eq("setLlegit",true,        l.getLlegit());
        l.setPortada("p/x.png");   eq("setPort",  "p/x.png",   l.getPortada());

        section("Llibre — toString");
        String s = l.toString();
        check("toString contains ISBN",  s.contains(isbn.toString()));
        check("toString contains nom",   s.contains("Nou nom"));
        check("toString contains autor", s.contains("Nou autor"));
    }

    // ── Tests: checkLlibre.countDig ───────────────────────────────────────────
    static void testCountDig() {
        section("checkLlibre — countDig");
        eq("1 digit",       1,  checkLlibre.countDig(5));
        eq("2 digits",      2,  checkLlibre.countDig(42));
        eq("10 digits",    10,  checkLlibre.countDig(1234567890L));
        eq("13 digits",    13,  checkLlibre.countDig(9788412345678L));
        eq("14 digits",    14,  checkLlibre.countDig(97884123456789L));
    }

    // ── Tests: checkLlibre.cheackLlibre ───────────────────────────────────────
    static void testCheackLlibre() {
        section("checkLlibre — cheackLlibre (valid ISBN)");
        // 14-digit ISBN → pass regardless of other fields
        Llibre l = checkLlibre.cheackLlibre(97884179104580L, "N", "A", 2024, "D", 5.0, 10.0, false, "portades/x.jpg");
        notNull("14-digit ISBN returns Llibre", l);
        eq("ISBN preserved", 97884179104580L, l == null ? null : l.getISBN());

        section("checkLlibre — cheackLlibre (valid valoracio, bad ISBN)");
        // valoracio 0-10 → pass
        Llibre l2 = checkLlibre.cheackLlibre(123L, "N", "A", 2024, "D", 0.0, 10.0, false, "nopath");
        notNull("valoracio=0 returns Llibre", l2);
        Llibre l3 = checkLlibre.cheackLlibre(123L, "N", "A", 2024, "D", 10.0, 10.0, false, "nopath");
        notNull("valoracio=10 returns Llibre", l3);

        section("checkLlibre — cheackLlibre (portada prefix)");
        Llibre l4 = checkLlibre.cheackLlibre(123L, "N", "A", 2024, "D", 99.0, 10.0, false, "portades/cover.png");
        notNull("portades/ prefix returns Llibre", l4);

        section("checkLlibre — cheackLlibre (all fail → null)");
        Llibre l5 = checkLlibre.cheackLlibre(123L, "N", "A", 2024, "D", 99.0, 10.0, false, "nopath");
        isNull("bad ISBN + bad valoracio + bad portada = null", l5);
    }

    // ── Tests: FiltreUtils.matchISBN ──────────────────────────────────────────
    static void testMatchISBN() {
        section("FiltreUtils — matchISBN");
        Long full = 97884179104580L;

        check("exact match",                FiltreUtils.matchISBN(full, full));
        check("4-digit prefix match",       FiltreUtils.matchISBN(9788L, full));
        check("10-digit prefix match",      FiltreUtils.matchISBN(9788417910L, full));
        check("non-matching prefix",       !FiltreUtils.matchISBN(1234L, full));
        check("1-digit match first digit",  FiltreUtils.matchISBN(9L, full));
        check("1-digit no match",          !FiltreUtils.matchISBN(8L, full));
        check("null ISBN → false",         !FiltreUtils.matchISBN(null, full));
        check("null book → false",         !FiltreUtils.matchISBN(9788L, null));
    }

    // ── Tests: FiltreUtils.matchString ────────────────────────────────────────
    static void testMatchString() {
        section("FiltreUtils — matchString");
        check("full match",          FiltreUtils.matchString("Hola", "Hola món"));
        check("end match",           FiltreUtils.matchString("món", "Hola món"));
        check("middle match",        FiltreUtils.matchString("la", "Hola món"));
        check("no match",           !FiltreUtils.matchString("xyz", "Hola món"));
        check("case sensitive fail", !FiltreUtils.matchString("hola", "Hola món"));
        check("empty needle",        FiltreUtils.matchString("", "qualsevol"));
        check("exact equal",         FiltreUtils.matchString("Exacte", "Exacte"));
        check("needle longer",      !FiltreUtils.matchString("MoltLlarg", "curt"));
        check("null needle → false", !FiltreUtils.matchString(null, "text"));
        check("null haystack → false",!FiltreUtils.matchString("text", null));
    }

    // ── Tests: filter predicate logic ─────────────────────────────────────────
    static void testFiltreLogic() {
        section("Filter predicate — any/valoracio/preu ranges");

        ArrayList<Llibre> bib = new ArrayList<>();
        bib.add(new Llibre(11111111111111L, "El Quixot",     "Cervantes", 1605, "", 9.0, 15.0, true,  "p/a.jpg"));
        bib.add(new Llibre(22222222222222L, "Tirant lo Blanc","Martorell",1490, "", 7.5, 12.0, false, "p/b.jpg"));
        bib.add(new Llibre(33333333333333L, "L'Odissea",     "Homer",      -800, "", 10.0, 8.0, true,  "p/c.jpg"));

        // Filter by year min
        ArrayList<Llibre> r1 = filtrar(bib, null, null, null, 1000, null, null, null, null, null);
        eq("any >= 1000: 2 results", 2, r1.size());

        // Filter by year max
        ArrayList<Llibre> r2 = filtrar(bib, null, null, null, null, 1500, null, null, null, null);
        eq("any <= 1500: 2 results", 2, r2.size());

        // Filter by year range
        ArrayList<Llibre> r3 = filtrar(bib, null, null, null, 1000, 1700, null, null, null, null);
        eq("1000 <= any <= 1700: 2 results", 2, r3.size());

        // Filter by valoracio min
        ArrayList<Llibre> r4 = filtrar(bib, null, null, null, null, null, 9.0, null, null, null);
        eq("valoracio >= 9: 2 results", 2, r4.size());

        // Filter by preu max
        ArrayList<Llibre> r5 = filtrar(bib, null, null, null, null, null, null, null, null, 10.0);
        eq("preu <= 10: 1 result", 1, r5.size());

        // Filter llegit=true
        ArrayList<Llibre> r6 = filtrar(bib, null, null, null, null, null, null, null, null, null);
        eq("no filter: 3 results", 3, r6.size());

        ArrayList<Llibre> r7 = filtrar(bib, null, null, null, null, null, null, null, null, null);
        eq("llegit filter not applied if null: 3 results", 3, r7.size());

        // Filter by nom substring
        ArrayList<Llibre> r8 = filtrar(bib, null, "Quixot", null, null, null, null, null, null, null);
        eq("nom 'Quixot': 1 result", 1, r8.size());

        // Filter by autor
        ArrayList<Llibre> r9 = filtrar(bib, "Homer", null, null, null, null, null, null, null, null);
        eq("autor 'Homer': 1 result", 1, r9.size());

        // Filter combined: llegit + year
        ArrayList<Llibre> r10 = filtrar(bib, null, null, null, 1000, null, null, null, null, null);
        eq("llegit=true + any>=1000 combined: correct", 2, r10.size());
    }

    // Mirrors ControladorDomini.aplicarFiltres for testing without DB
    static ArrayList<Llibre> filtrar(ArrayList<Llibre> bib,
            String nomAutor, String nomLlibre, Long ISBN,
            Integer iniciAny, Integer fiAny,
            Double valoracioMin, Double valoracioMax,
            Double preuMin, Double preuMax) {
        ArrayList<Llibre> res = new ArrayList<>();
        for (Llibre l : bib) {
            if ((nomAutor    == null || FiltreUtils.matchString(nomAutor, l.getAutor()))
             && (nomLlibre   == null || FiltreUtils.matchString(nomLlibre, l.getNom()))
             && (ISBN        == null || FiltreUtils.matchISBN(ISBN, l.getISBN()))
             && (iniciAny    == null || l.getAny() >= iniciAny)
             && (fiAny       == null || l.getAny() <= fiAny)
             && (valoracioMin== null || l.getValoracio() >= valoracioMin)
             && (valoracioMax== null || l.getValoracio() <= valoracioMax)
             && (preuMin     == null || l.getPreu() >= preuMin)
             && (preuMax     == null || l.getPreu() <= preuMax))
                res.add(l);
        }
        return res;
    }
}
