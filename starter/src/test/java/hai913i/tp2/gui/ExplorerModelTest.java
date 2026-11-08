package hai913i.tp2.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Linkage;
import hai913i.tp2.clustering.TreeComparison;
import hai913i.tp2.input.Facts;
import hai913i.tp2.input.Inheritance;

/**
 * Bonus C3 : l'état de l'interface graphique, testé sans écran.
 */
class ExplorerModelTest {

    private static final Path DIR = Path.of("src", "test", "resources", "validation");

    private static ExplorerModel model(boolean withLinks) throws IOException {
        List<Inheritance.Link> links = withLinks
                ? Inheritance.load(Inheritance.jsonFile(DIR, "facts-validation")) : null;
        return new ExplorerModel(Facts.load(DIR), links, 0.025);
    }

    @Test
    void changerCpNeRefaitQueLaCoupe() throws IOException {
        ExplorerModel m = model(false);
        Dendrogram before = m.dendrogram();
        assertEquals(7, m.cut().modules().size());
        assertTrue(m.cut().respectsMaxModules());

        m.setCp(0.028);                    // au-dessus de C̄(G20) = 0,0270
        assertSame(before, m.dendrogram());
        assertEquals(8, m.cut().modules().size());
        assertFalse(m.cut().respectsMaxModules());
    }

    @Test
    void unCpInvalideEstRefuseEtLEtatNeChangePas() throws IOException {
        ExplorerModel m = model(false);
        assertThrows(IllegalArgumentException.class, () -> m.setCp(-0.01));
        assertThrows(IllegalArgumentException.class, () -> m.setCp(Double.NaN));
        assertEquals(0.025, m.cp());
        assertEquals(7, m.cut().modules().size());
    }

    @Test
    void changerDeLienReconstruitLArbre() throws IOException {
        ExplorerModel m = model(false);
        Dendrogram moyen = m.dendrogram();
        m.setLinkage(Linkage.SIMPLE);
        assertEquals(2, TreeComparison.firstDivergence(moyen, m.dendrogram()));
    }

    @Test
    void lHeritageNEstDisponibleQueSiLeJsonAEteLu() throws IOException {
        ExplorerModel sans = model(false);
        assertFalse(sans.heritageAvailable());
        assertThrows(IllegalStateException.class, () -> sans.setHeritage(true));

        ExplorerModel avec = model(true);
        avec.setHeritage(true);
        assertEquals(41, avec.coupling().interArcs());
        assertEquals(8, avec.cut().modules().size());   // même résultat qu'en ligne de commande
    }

    @Test
    void chaqueNoeudSaitDansQuelModuleIlEst() throws IOException {
        ExplorerModel m = model(false);
        List<String> labels = m.dendrogram().labels();
        int member = labels.indexOf("library.model.Member");
        int console = labels.indexOf("library.service.ConsoleNotifier");
        assertEquals(m.moduleOfClass(member), m.moduleOfClass(console));
        assertEquals(-1, m.moduleOfNode(m.dendrogram().root()));   // la racine est refusée
    }

    @Test
    void laRechercheTrouveLesClassesParMorceauDeNom() throws IOException {
        ExplorerModel m = model(false);
        assertEquals(4, m.search("loan").size());   // Loanable, LoanException, LoanService, Loan
        assertTrue(m.search("  ").isEmpty());
    }

    @Test
    void leDetailMontreLaFusionEtLeCouplageInterne() throws IOException {
        ExplorerModel m = model(false);
        Cluster g22 = m.dendrogram().allNodes().stream()
                .filter(c -> c.id() == 22).findFirst().orElseThrow();
        String text = m.describe(g22);
        assertTrue(text.contains("0.0090"), text);   // similarité de la fusion
        assertTrue(text.contains("0.0335"), text);   // couplage interne moyen
    }
}
