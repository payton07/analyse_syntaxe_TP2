package hai913i.tp2.coupling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import hai913i.tp2.input.Facts;

/**
 * Invariants du couplage (annexe § 6) et point de contrôle A1.
 *
 * <p>Micro jeu de données : valeurs calculées à la main dans
 * {@code examples/README.txt}. Projet de validation : chiffres du point de
 * contrôle A1 du sujet.</p>
 */
class CouplingTest {

    private static final double EPS = 1e-12;

    private static Coupling mini() throws IOException {
        return Coupling.from(Facts.load(Path.of("src", "test", "resources"), "facts-mini"));
    }

    private static Coupling validation() throws IOException {
        return Coupling.from(Facts.load(Path.of("src", "test", "resources", "validation")));
    }

    // ---------------------------------------------------------------- micro

    @Test
    void miniEcarteUnArcInterne() throws IOException {
        Coupling c = mini();
        assertEquals(1, c.internalArcs());
        assertEquals(3, c.interArcs());
    }

    @Test
    void miniValeursCalculeesALaMain() throws IOException {
        Coupling c = mini();
        assertEquals(2.0 / 3, c.c("demo.A", "demo.B"), EPS);
        assertEquals(1.0 / 3, c.c("demo.B", "demo.C"), EPS);
        assertEquals(0.0, c.c("demo.A", "demo.C"), EPS);
        assertEquals(2, c.r("demo.A", "demo.B"));   // 2 arcs, mais 3 sites
    }

    @Test
    void miniSymetrieEtDiagonaleNulle() throws IOException {
        assertSymetrieEtDiagonale(mini());
    }

    @Test
    void miniSommeVautUn() throws IOException {
        assertEquals(1.0, mini().total(), EPS);
    }

    // ----------------------------------------------------------- validation

    @Test
    void validationPointDeControleA1() throws IOException {
        Coupling c = validation();
        assertEquals(15, c.classes().size());
        assertEquals(7, c.internalArcs());
        assertEquals(37, c.interArcs());
        assertEquals(19, c.nonZeroPairs().size());
        assertEquals(105, c.possiblePairs());
        assertEquals(1.0, c.total(), EPS);
    }

    @Test
    void validationCoupleLePlusCouple() throws IOException {
        Coupling c = validation();
        Map.Entry<ClassPair, Integer> top = c.byDecreasingCoupling().get(0);
        assertEquals(ClassPair.of("library.model.Item", "library.service.LoanService"),
                top.getKey());
        assertEquals(4, top.getValue());
        assertEquals(4.0 / 37, c.c("library.service.LoanService", "library.model.Item"), EPS);
    }

    @Test
    void validationDvdEtLoanExceptionNApparaissentDansAucunCouple() throws IOException {
        Coupling c = validation();
        for (ClassPair p : c.nonZeroPairs().keySet()) {
            for (String isolee : List.of("library.model.Dvd", "library.service.LoanException")) {
                assertFalse(p.first().equals(isolee) || p.second().equals(isolee), p.toString());
            }
        }
    }

    @Test
    void validationSymetrieDiagonaleEtBornes() throws IOException {
        assertSymetrieEtDiagonale(validation());
    }

    // --------------------------------------------------------------- ClassPair

    @Test
    void unCoupleEstNonOriente() {
        assertEquals(ClassPair.of("x.B", "x.A"), ClassPair.of("x.A", "x.B"));
        assertEquals("x.A", ClassPair.of("x.B", "x.A").first());
    }

    @Test
    void unCoupleReflexifEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> ClassPair.of("x.A", "x.A"));
    }

    // ---------------------------------------------------------------- outils

    /** C(A,B) = C(B,A), C(A,A) = 0, et 0 ≤ C ≤ 1 pour tous les couples. */
    private static void assertSymetrieEtDiagonale(Coupling c) {
        for (String a : c.classes()) {
            assertEquals(0.0, c.c(a, a), EPS, "diagonale " + a);
            for (String b : c.classes()) {
                double ab = c.c(a, b);
                assertEquals(ab, c.c(b, a), EPS, a + " / " + b);
                assertTrue(ab >= 0.0 && ab <= 1.0, a + " / " + b + " = " + ab);
            }
        }
    }
}
