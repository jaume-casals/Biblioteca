package domini;

public class BibliotecaException extends RuntimeException {

    public enum Code { UNKNOWN, NOT_FOUND, DUPLICATE, VALIDATION }

    private final Code code;

    public BibliotecaException(String message) { this(message, Code.UNKNOWN, null); }
    public BibliotecaException(String message, Throwable cause) { this(message, Code.UNKNOWN, cause); }
    protected BibliotecaException(String message, Code code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code code() { return code; }

    public static class NotFound extends BibliotecaException {
        public NotFound(String message) { super(message, Code.NOT_FOUND, null); }
    }
    public static class Duplicate extends BibliotecaException {
        public Duplicate(String message) { super(message, Code.DUPLICATE, null); }
    }
    public static class Validation extends BibliotecaException {
        public Validation(String message) { super(message, Code.VALIDATION, null); }
    }
}