package hai913i.tp2.output;

import java.util.ArrayList;
import java.util.List;

/**
 * Libellés lisibles à partir des noms qualifiés, pour tous les affichages.
 *
 * <p>Convention Java : les paquetages sont en minuscules, les classes commencent
 * par une majuscule. Le nom court d'une classe interne garde sa classe
 * englobante : {@code library.service.LoanService.Loan} → {@code LoanService.Loan},
 * paquetage {@code library.service}.</p>
 */
public final class Names {

    private Names() {
    }

    /** Position du premier segment qui commence par une majuscule. */
    private static int classStart(String qualified) {
        int pos = 0;
        for (String s : qualified.split("\\.")) {
            if (!s.isEmpty() && Character.isUpperCase(s.charAt(0))) {
                return pos;
            }
            pos += s.length() + 1;
        }
        return qualified.lastIndexOf('.') + 1;
    }

    /**
     * @param qualified nom qualifié
     * @return le nom court, classe englobante comprise pour une classe interne
     */
    public static String simple(String qualified) {
        return qualified.substring(classStart(qualified));
    }

    /**
     * @param qualified nom qualifié
     * @return le paquetage, ou {@code (défaut)} s'il n'y en a pas
     */
    public static String packageOf(String qualified) {
        int start = classStart(qualified);
        return start == 0 ? "(défaut)" : qualified.substring(0, start - 1);
    }

    /**
     * @param qualified noms qualifiés
     * @return « {A, B, C} » avec les noms courts
     */
    public static String braces(List<String> qualified) {
        List<String> s = new ArrayList<>();
        for (String q : qualified) {
            s.add(simple(q));
        }
        return "{" + String.join(", ", s) + "}";
    }
}
