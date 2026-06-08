package checkBiblio;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;

public class RunI18nAudit {
    public static void main(String[] args) throws Exception {
        try (PrintWriter log = new PrintWriter(new FileWriter("checkBiblio/i18n_audit_test.txt", false), true)) {
            int[] fail = {0}, warn = {0};
            I18nAudit.run(log, fail, warn);
            System.out.println("FAIL=" + fail[0] + " WARN=" + warn[0]);
        }
    }
}
