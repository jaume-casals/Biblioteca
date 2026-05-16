package domini;

/** Loan record — one row from the `prestec` table, including returned loans. */
public record PrestecRow(long isbn, String nomPersona, String dataPrestec, boolean retornat) {}
