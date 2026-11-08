package hai913i.tp2.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.input.Facts;

/** Exports du dendrogramme (B2), vérifiés sur le micro jeu de données. */
class DendrogramExportTest {

    private static Dendrogram mini() throws IOException {
        Coupling c = Coupling.from(Facts.load(Path.of("src", "test", "resources"), "facts-mini"));
        return HierarchicalClustering.run(c.classes(), Similarity.fromMatrix(c.matrix()));
    }

    @Test
    void listeDesFusionsDuMicroJeu() throws IOException {
        assertEquals("""
                etape;similarite;idA;idB;nouveau;tailleA;tailleB;membresA;membresB
                1;0.666667;0;1;3;1;1;A;B
                2;0.166667;2;3;4;1;2;C;A,B
                """, DendrogramExport.mergesCsv(mini()));
    }

    @Test
    void arbreIndenteDuMicroJeu() throws IOException {
        assertEquals("""
                + G4  fusion 2 à 0.1667  3 classes {A, B, C}
                    - C  (#2)
                    + G3  fusion 1 à 0.6667  2 classes {A, B}
                        - A  (#0)
                        - B  (#1)
                """, DendrogramExport.indentedTree(mini()));
    }

    @Test
    void dotContientToutesLesAretesDeLArbre() throws IOException {
        String dot = DendrogramExport.dot(mini());
        assertTrue(dot.startsWith("digraph dendrogramme"));
        assertTrue(dot.contains("G0 -> G3;") && dot.contains("G1 -> G3;"));
        assertTrue(dot.contains("G3 -> G4;") && dot.contains("G2 -> G4;"));
    }

    @Test
    void ecritLesTroisFichiers(@TempDir Path tmp) throws IOException {
        List<Path> files = DendrogramExport.writeAll(mini(), tmp);
        assertEquals(3, files.size());
        for (Path f : files) {
            assertTrue(Files.size(f) > 0, f.toString());
        }
    }

    @Test
    void nomsCourtsEtPaquetages() {
        assertEquals("LoanService.Loan", Names.simple("library.service.LoanService.Loan"));
        assertEquals("library.service", Names.packageOf("library.service.LoanService.Loan"));
        assertEquals("Item", Names.simple("library.model.Item"));
    }
}
