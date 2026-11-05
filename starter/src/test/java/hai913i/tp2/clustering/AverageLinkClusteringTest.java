package hai913i.tp2.clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.input.Facts;

/**
 * Invariants du regroupement et de l'arbre (annexe § 6), points de contrôle B1 et B2.
 */
class AverageLinkClusteringTest {

    private static final double EPS = 1e-12;

    private static Coupling couplingMini() throws IOException {
        return Coupling.from(Facts.load(Path.of("src", "test", "resources"), "facts-mini"));
    }

    private static Coupling couplingValidation() throws IOException {
        return Coupling.from(Facts.load(Path.of("src", "test", "resources", "validation")));
    }

    private static Dendrogram run(Coupling c) {
        return AverageLinkClustering.run(c.classes(), Similarity.fromMatrix(c.matrix()));
    }

    private static List<String> labels(int m) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            l.add("c" + i);
        }
        return l;
    }

    // --------------------------------------------------------- micro jeu

    @Test
    void miniDeuxFusionsCalculeesALaMain() throws IOException {
        Dendrogram d = run(couplingMini());
        assertEquals(2, d.merges().size());
        Merge m1 = d.merges().get(0);
        assertEquals(List.of(0), m1.a().members());          // {A}
        assertEquals(List.of(1), m1.b().members());          // {B}
        assertEquals(2.0 / 3, m1.similarity(), EPS);
        Merge m2 = d.merges().get(1);
        // couple (min, max) des identifiants : {C} = 2 < {A, B} = 3, donc {C} est « groupe A »
        assertEquals(List.of(2), m2.a().members());          // {C}
        assertEquals(List.of(0, 1), m2.b().members());       // {A, B}
        assertEquals((0 + 1.0 / 3) / 2, m2.similarity(), EPS);   // 0,1667 : le zéro A–C compte
    }

    // ---------------------------------------------------- départage

    @Test
    void departageRetientLePlusPetitCouple() {
        // trois couples à 0,25 : (0,2), (1,3), (2,3) → on retient (0,2)
        double[][] x = new double[4][4];
        x[0][2] = x[2][0] = 0.25;
        x[1][3] = x[3][1] = 0.25;
        x[2][3] = x[3][2] = 0.25;
        x[0][1] = x[1][0] = 0.10;
        Merge first = AverageLinkClustering.run(labels(4), Similarity.fromMatrix(x)).merges().get(0);
        assertEquals(0, first.a().id());
        assertEquals(2, first.b().id());
    }

    @Test
    void departageComparerLeSecondIdentifiant() {
        // (0,3) et (0,2) à égalité : même premier identifiant, 2 < 3 → (0,2)
        double[][] x = new double[4][4];
        x[0][3] = x[3][0] = 0.5;
        x[0][2] = x[2][0] = 0.5;
        Merge first = AverageLinkClustering.run(labels(4), Similarity.fromMatrix(x)).merges().get(0);
        assertEquals(0, first.a().id());
        assertEquals(2, first.b().id());
    }

    @Test
    void egaliteAuDernierBitPresEstUneEgalite() {
        // 1/37 et (0 + 2/37)/2 : mathématiquement égaux, peut-être pas au bit près
        double[][] x = new double[3][3];
        x[1][2] = x[2][1] = (0 + 2.0 / 37) / 2;
        x[0][1] = x[1][0] = 1.0 / 37;
        Merge first = AverageLinkClustering.run(labels(3), Similarity.fromMatrix(x)).merges().get(0);
        assertEquals(0, first.a().id());     // (0,1) gagne le départage, quel que soit le dernier bit
        assertEquals(1, first.b().id());
    }

    // ------------------------------------------- projet de validation : B1

    @Test
    void validationQuatorzeFusionsEtTroisPremieres() throws IOException {
        Coupling c = couplingValidation();
        Dendrogram d = run(c);
        assertEquals(14, d.merges().size());

        Merge m1 = d.merges().get(0);
        assertEquals(List.of("library.model.Item"), d.labelsOf(m1.a()));
        assertEquals(List.of("library.service.LoanService"), d.labelsOf(m1.b()));
        assertEquals(4.0 / 37, m1.similarity(), EPS);

        Merge m2 = d.merges().get(1);
        assertEquals(List.of("library.app.LibraryApp"), d.labelsOf(m2.a()));
        assertEquals(List.of("library.service.Catalog"), d.labelsOf(m2.b()));
        assertEquals(3.0 / 37, m2.similarity(), EPS);

        Merge m3 = d.merges().get(2);
        assertEquals(Set.of("library.model.Item", "library.service.LoanService"),
                new HashSet<>(d.labelsOf(m3.a()).size() == 2 && d.labelsOf(m3.a()).contains("library.model.Item")
                        ? d.labelsOf(m3.a()) : d.labelsOf(m3.b())));
        assertEquals(9.0 / 148, m3.similarity(), EPS);     // (3 + 3 + 2 + 1) / (4 × 37)
    }

    @Test
    void validationSimilaritesNeRemontentJamaisEtDeuxDernieresNulles() throws IOException {
        List<Merge> ms = run(couplingValidation()).merges();
        for (int k = 1; k < ms.size(); k++) {
            assertTrue(ms.get(k).similarity() <= ms.get(k - 1).similarity() + AverageLinkClustering.EPS,
                    "remontée à l'étape " + (k + 1));
        }
        assertEquals(0.0, ms.get(12).similarity(), EPS);
        assertEquals(0.0, ms.get(13).similarity(), EPS);
    }

    @Test
    void validationGroupesActifsFormentUnePartitionAChaqueEtape() throws IOException {
        Dendrogram d = run(couplingValidation());
        int m = 15;
        TreeMap<Integer, Cluster> active = new TreeMap<>();
        for (int i = 0; i < m; i++) {
            active.put(i, Cluster.leaf(i));
        }
        for (Merge mg : d.merges()) {
            assertTrue(active.containsKey(mg.a().id()) && active.containsKey(mg.b().id()),
                    "fusion d'un groupe inactif à l'étape " + mg.step());
            active.remove(mg.a().id());
            active.remove(mg.b().id());
            active.put(mg.result().id(), mg.result());
            List<Integer> all = new ArrayList<>();
            active.values().forEach(g -> all.addAll(g.members()));
            all.sort(null);
            assertEquals(range(m), all, "partition à l'étape " + mg.step());
        }
        assertEquals(1, active.size());
    }

    @Test
    void lienMoyenDirectEgaleLaFormuleIncrementale() throws IOException {
        Coupling c = couplingValidation();
        Similarity sim = Similarity.fromMatrix(c.matrix());
        Dendrogram d = run(c);
        TreeMap<Integer, Cluster> active = new TreeMap<>();
        for (int i = 0; i < 15; i++) {
            active.put(i, Cluster.leaf(i));
        }
        for (Merge mg : d.merges()) {
            active.remove(mg.a().id());
            active.remove(mg.b().id());
            for (Cluster h : active.values()) {
                double direct = AverageLinkClustering.averageLink(mg.result(), h, sim);
                double incr = (mg.a().size() * AverageLinkClustering.averageLink(mg.a(), h, sim)
                        + mg.b().size() * AverageLinkClustering.averageLink(mg.b(), h, sim))
                        / (mg.a().size() + mg.b().size());
                assertEquals(incr, direct, 1e-12, "étape " + mg.step() + ", groupe " + h);
            }
            active.put(mg.result().id(), mg.result());
        }
    }

    @Test
    void deuxExecutionsDonnentLaMemeSuiteDeFusions() throws IOException {
        List<String> first = signature(run(couplingValidation()));
        List<String> second = signature(run(couplingValidation()));
        assertEquals(first, second);
    }

    // ----------------------------------------------- projet de validation : B2

    @Test
    void arbreQuinzeFeuillesQuatorzeNoeudsInternes() throws IOException {
        Dendrogram d = run(couplingValidation());
        List<Cluster> nodes = d.allNodes();
        assertEquals(15, nodes.stream().filter(Cluster::isLeaf).count());
        assertEquals(14, nodes.stream().filter(n -> !n.isLeaf()).count());
        assertEquals(29, nodes.stream().map(Cluster::id).distinct().count());   // aucun nœud en double
        assertEquals(range(15), d.root().members());
    }

    @Test
    void membresDUnNoeudSontLUnionDisjointeDeSesFils() throws IOException {
        for (Cluster n : run(couplingValidation()).allNodes()) {
            if (n.isLeaf()) {
                continue;
            }
            Set<Integer> l = new HashSet<>(n.left().members());
            Set<Integer> r = new HashSet<>(n.right().members());
            assertFalse(l.removeIf(r::contains), "fils non disjoints dans " + n);
            List<Integer> union = new ArrayList<>(n.left().members());
            union.addAll(n.right().members());
            union.sort(null);
            assertEquals(union, n.members(), n.toString());
        }
    }

    // ------------------------------------------------------------- outils

    private static List<Integer> range(int m) {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            r.add(i);
        }
        return r;
    }

    private static List<String> signature(Dendrogram d) {
        List<String> s = new ArrayList<>();
        for (Merge m : d.merges()) {
            s.add(m.step() + ";" + m.a().id() + ";" + m.b().id() + ";" + m.similarity());
        }
        return s;
    }
}
