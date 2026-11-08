package hai913i.tp2.clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.input.Facts;

/**
 * Bonus C1 : le lien simple, et la comparaison avec le lien moyen.
 */
class LinkageTest {

    private static final double EPS = 1e-12;

    private static Coupling validation() throws IOException {
        return Coupling.from(Facts.load(Path.of("src", "test", "resources", "validation")));
    }

    @Test
    void lienSimplePrendLeMeilleurCoupleEtLienMoyenLaMoyenne() {
        // groupe {0, 1} face à {2} : C(0,2) = 0,1 et C(1,2) = 0,3
        double[][] x = {
            {0.0, 0.5, 0.1},
            {0.5, 0.0, 0.3},
            {0.1, 0.3, 0.0},
        };
        Similarity sim = Similarity.fromMatrix(x);
        Cluster g = Cluster.merge(3, Cluster.leaf(0), Cluster.leaf(1), 0.5, 1);
        assertEquals(0.3, Linkage.SIMPLE.between(g, Cluster.leaf(2), sim), EPS);
        assertEquals(0.2, Linkage.MOYEN.between(g, Cluster.leaf(2), sim), EPS);
    }

    @Test
    void parDefautLeRegroupementEstAuLienMoyen() throws IOException {
        Coupling c = validation();
        Similarity sim = Similarity.fromMatrix(c.matrix());
        Dendrogram d1 = HierarchicalClustering.run(c.classes(), sim);
        Dendrogram d2 = HierarchicalClustering.run(c.classes(), sim, Linkage.MOYEN);
        assertEquals(0, TreeComparison.firstDivergence(d1, d2));
    }

    @Test
    void surLaValidationLesDeuxArbresDivergentALEtape2() throws IOException {
        Coupling c = validation();
        Similarity sim = Similarity.fromMatrix(c.matrix());
        Dendrogram moyen = HierarchicalClustering.run(c.classes(), sim, Linkage.MOYEN);
        Dendrogram simple = HierarchicalClustering.run(c.classes(), sim, Linkage.SIMPLE);
        assertEquals(2, TreeComparison.firstDivergence(moyen, simple));

        // au lien simple, Loan rejoint {Item, LoanService} grâce à son seul meilleur couple : 4/37
        Merge step2 = simple.merges().get(1);
        assertEquals(4.0 / 37, step2.similarity(), EPS);
        assertEquals(List.of("library.service.LoanService.Loan"), simple.labelsOf(step2.a()));
        assertEquals(List.of("library.model.Item", "library.service.LoanService"), simple.labelsOf(step2.b()));
    }

    @Test
    void auLienSimpleLesSimilaritesNeRemontentJamaisNonPlus() throws IOException {
        Coupling c = validation();
        Dendrogram d = HierarchicalClustering.run(c.classes(), Similarity.fromMatrix(c.matrix()), Linkage.SIMPLE);
        List<Merge> ms = d.merges();
        assertEquals(14, ms.size());
        for (int k = 1; k < ms.size(); k++) {
            assertTrue(ms.get(k).similarity() <= ms.get(k - 1).similarity() + EPS, "étape " + (k + 1));
        }
    }

    @Test
    void leCritereSeLitSurLaLigneDeCommande() {
        assertEquals(Linkage.SIMPLE, Linkage.parse("simple"));
        assertEquals(Linkage.MOYEN, Linkage.parse(" Moyen "));
        assertNull(Linkage.parse("complet"));
    }
}
