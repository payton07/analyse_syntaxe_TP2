package hai913i.tp2.cut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import hai913i.tp2.clustering.AverageLinkClustering;
import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.input.Facts;

/** Invariants de la coupe (annexe § 6) et point de contrôle B3. */
class TreeCutTest {

    private static final double EPS = 1e-12;

    /** Un jeu de données prêt à couper : arbre + similarité. */
    private record Setup(Dendrogram d, Similarity sim) {
        CutResult cut(double cp) {
            return TreeCut.cut(d, sim, cp);
        }
    }

    private static Setup setup(Path dir) throws IOException {
        Coupling c = Coupling.from(Facts.load(dir));
        Similarity sim = Similarity.fromMatrix(c.matrix());
        return new Setup(AverageLinkClustering.run(c.classes(), sim), sim);
    }

    private static Setup mini() throws IOException {
        Coupling c = Coupling.from(Facts.load(Path.of("src", "test", "resources"), "facts-mini"));
        Similarity sim = Similarity.fromMatrix(c.matrix());
        return new Setup(AverageLinkClustering.run(c.classes(), sim), sim);
    }

    private static Setup validation() throws IOException {
        return setup(Path.of("src", "test", "resources", "validation"));
    }

    // ------------------------------------------------------------ micro jeu

    @Test
    void miniCp020UnModuleContrainteRespectee() throws IOException {
        CutResult r = mini().cut(0.20);
        assertEquals(1, r.modules().size());
        assertEquals(3, r.modules().get(0).size());
        assertEquals(1.0 / 3, r.modules().get(0).internalCoupling(), EPS);   // (2/3 + 0 + 1/3) / 3
        assertTrue(r.respectsMaxModules());
    }

    @Test
    void miniCp050DeuxModulesContrainteViolee() throws IOException {
        CutResult r = mini().cut(0.50);
        assertEquals(2, r.modules().size());
        assertEquals(1, r.maxModules());
        assertFalse(r.respectsMaxModules());
    }

    @Test
    void egaliteAvecCpNEstPasStrictementSuperieure() throws IOException {
        // C̄(racine) = 1/3 exactement : « > » strict, donc on descend
        CutResult r = mini().cut(1.0 / 3);
        assertEquals(2, r.modules().size());
    }

    @Test
    void cpInvalideEstRefuse() throws IOException {
        Setup s = mini();
        assertThrows(IllegalArgumentException.class, () -> s.cut(-0.1));
        assertThrows(IllegalArgumentException.class, () -> s.cut(Double.NaN));
    }

    // -------------------------------------------------- validation : B3

    @Test
    void validationCp0015QuatreModulesDontUnDeOnze() throws IOException {
        CutResult r = validation().cut(0.015);
        assertEquals(4, r.modules().size());
        assertEquals(11, r.largestModuleSize());
        assertTrue(r.respectsMaxModules());
    }

    @Test
    void validationCp0025SeptModulesToutJuste() throws IOException {
        CutResult r = validation().cut(0.025);
        assertEquals(7, r.modules().size());
        assertEquals(7, r.maxModules());
        assertTrue(r.respectsMaxModules());
        // module principal : couplage interne moyen 0,0335 (annexe § 4.1)
        CutResult.Module principal = r.modules().stream()
                .filter(m -> m.size() == r.largestModuleSize()).findFirst().orElseThrow();
        assertEquals(0.0335, principal.internalCoupling(), 5e-5);
        // … alors que la fusion qui l'a créé s'est faite bien plus bas
        assertTrue(principal.cluster().similarity() < principal.internalCoupling());
    }

    @Test
    void validationCp0045DixModulesContrainteViolee() throws IOException {
        CutResult r = validation().cut(0.045);
        assertEquals(10, r.modules().size());
        assertFalse(r.respectsMaxModules());
    }

    // --------------------------------------------- invariants de la coupe

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.015, 0.025, 0.035, 0.045, 0.1, 1.0})
    void modulesFormentUnePartitionDeBranchesAuCouplageSuffisant(double cp) throws IOException {
        Setup s = validation();
        CutResult r = s.cut(cp);
        // partition des 15 classes
        List<Integer> all = new ArrayList<>();
        r.modules().forEach(m -> all.addAll(m.cluster().members()));
        all.sort(null);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            expected.add(i);
        }
        assertEquals(expected, all, "partition à CP = " + cp);
        // chaque module est un nœud (une branche) de l'arbre
        Set<Integer> nodeIds = new HashSet<>();
        s.d().allNodes().forEach(n -> nodeIds.add(n.id()));
        for (CutResult.Module m : r.modules()) {
            assertTrue(nodeIds.contains(m.cluster().id()));
            if (m.size() >= 2) {
                assertTrue(m.internalCoupling() > cp, m.cluster() + " à CP = " + cp);
                assertEquals(TreeCut.internalCoupling(m.cluster(), s.sim()), m.internalCoupling(), EPS);
            } else {
                assertTrue(Double.isNaN(m.internalCoupling()));
            }
        }
    }

    @Test
    void couplageInterneMoyenNEstPasLaSimilariteDeFusion() throws IOException {
        Setup s = validation();
        int differents = 0;
        for (Cluster n : s.d().allNodes()) {
            if (!n.isLeaf() && Math.abs(TreeCut.internalCoupling(n, s.sim()) - n.similarity()) > 1e-9) {
                differents++;
            }
        }
        assertTrue(differents > 0);
    }

    @Test
    void cpNulDonneUnSeulModuleSiLaRacineEstCouplee() throws IOException {
        // CP = 0 : la racine (C̄ > 0) est acceptée d'emblée
        assertEquals(1, validation().cut(0.0).modules().size());
    }
}
