package domini;

public record LlibreLlistaContext(int isbn, int llistaId, Double valoracio, Boolean llegit) {
    public static LlibreLlistaContext of(int isbn, int llistaId, Double valoracio, Boolean llegit) {
        return new LlibreLlistaContext(isbn, llistaId, valoracio, llegit);
    }
}
