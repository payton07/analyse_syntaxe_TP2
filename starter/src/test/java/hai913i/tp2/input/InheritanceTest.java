package hai913i.tp2.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.cut.TreeCut;

/**
 * Bonus C2 : lecture de l'héritage et effet sur le couplage et les modules.
 */
class InheritanceTest {

    private static final Path DIR = Path.of("src", "test", "resources", "validation");

    private static List<Inheritance.Link> links() throws IOException {
        return Inheritance.load(Inheritance.jsonFile(DIR, "facts-validation"));
    }

    @Test
    void leBlocExtraDonneSeptLiensDontQuatreDansLeProjet() throws IOException {
        List<Inheritance.Link> l = links();
        assertEquals(7, l.size());
        assertTrue(l.contains(new Inheritance.Link("library.model.Dvd", "library.model.Item", false)));
        assertTrue(l.contains(new Inheritance.Link("library.model.Item", "library.model.Loanable", true)));

        Coupling c = Coupling.from(Facts.load(DIR), l);
        assertEquals(4, c.inheritanceLinks());          // Book, Dvd, Item, ConsoleNotifier
        assertEquals(3, c.ignoredInheritanceLinks());   // Enum, Exception, Throwable
    }

    @Test
    void lHeritageAjouteDesArcsEtLaSommeResteEgaleA1() throws IOException {
        Facts f = Facts.load(DIR);
        Coupling plain = Coupling.from(f);
        Coupling c = Coupling.from(f, links());
        assertEquals(37, plain.interArcs());
        assertEquals(41, c.interArcs());
        assertEquals(1.0, c.total(), 1e-9);
        // Dvd n'est plus isolée : un lien d'héritage vers Item
        assertEquals(0, plain.r("library.model.Dvd", "library.model.Item"));
        assertEquals(1, c.r("library.model.Dvd", "library.model.Item"));
        // Book–Item : l'appel existant plus le lien d'héritage
        assertEquals(plain.r("library.model.Book", "library.model.Item") + 1,
                c.r("library.model.Book", "library.model.Item"));
    }

    @Test
    void sansOptionLaRelationImposeeEstInchangee() throws IOException {
        Facts f = Facts.load(DIR);
        assertEquals(Coupling.from(f).nonZeroPairs(), Coupling.from(f, List.of()).nonZeroPairs());
    }

    @Test
    void avecLHeritageLaCoupeA0025VioleM2() throws IOException {
        Coupling c = Coupling.from(Facts.load(DIR), links());
        Similarity sim = Similarity.fromMatrix(c.matrix());
        Dendrogram d = HierarchicalClustering.run(c.classes(), sim);
        CutResult at0025 = TreeCut.cut(d, sim, 0.025);
        CutResult at002 = TreeCut.cut(d, sim, 0.02);
        assertEquals(8, at0025.modules().size());
        assertEquals(false, at0025.respectsMaxModules());
        assertEquals(7, at002.modules().size());
        assertEquals(true, at002.respectsMaxModules());
    }

    @Test
    void unJsonAbsentOuInvalideEstSignale(@TempDir Path dir) throws IOException {
        IOException absent = assertThrows(IOException.class,
                () -> Inheritance.load(dir.resolve("rien.json")));
        assertTrue(absent.getMessage().contains("introuvable"), absent.getMessage());

        Path bad = dir.resolve("bad.json");
        Files.writeString(bad, "{ \"extra\": { \"inheritance\": [ { \"type\": 3 ", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Inheritance.load(bad));

        Path noExtra = dir.resolve("vide.json");
        Files.writeString(noExtra, "{ \"classes\": [] }", StandardCharsets.UTF_8);
        IOException e = assertThrows(IOException.class, () -> Inheritance.load(noExtra));
        assertTrue(e.getMessage().contains("extra.inheritance"), e.getMessage());
    }
}
