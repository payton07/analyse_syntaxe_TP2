package hai913i.tp2.graph;

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

import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.input.Facts;

/** Point de contrôle A2 : graphe, top 5, isolées, exports et relecture. */
class CouplingGraphTest {

    private static final double EPS = 1e-12;

    private static CouplingGraph mini() throws IOException {
        return CouplingGraph.from(Coupling.from(
                Facts.load(Path.of("src", "test", "resources"), "facts-mini")));
    }

    private static CouplingGraph validation() throws IOException {
        return CouplingGraph.from(Coupling.from(
                Facts.load(Path.of("src", "test", "resources", "validation"))));
    }

    @Test
    void validationQuinzeNoeudsDixNeufAretes() throws IOException {
        CouplingGraph g = validation();
        assertEquals(15, g.nodes().size());
        assertEquals(19, g.edges().size());
    }

    @Test
    void validationCinqAretesLesPlusLourdes() throws IOException {
        List<CouplingGraph.Edge> top = validation().topEdges(5);
        List<String> attendu = List.of(
                "library.model.Item|library.service.LoanService|4",
                "library.service.LoanService|library.service.LoanService.Loan|4",
                "library.app.LibraryApp|library.model.Item|3",
                "library.app.LibraryApp|library.service.Catalog|3",
                "library.app.LibraryApp|library.service.LoanService|3");
        assertEquals(attendu, top.stream().map(e -> e.a() + "|" + e.b() + "|" + e.r()).toList());
        assertEquals(4.0 / 37, top.get(0).weight(), EPS);
    }

    @Test
    void validationDeuxClassesIsolees() throws IOException {
        assertEquals(List.of("library.model.Dvd", "library.service.LoanException"),
                validation().isolated());
    }

    @Test
    void poidsSymetriqueEtNulSiAbsent() throws IOException {
        CouplingGraph g = validation();
        assertEquals(g.weight("library.model.Item", "library.service.LoanService"),
                g.weight("library.service.LoanService", "library.model.Item"), EPS);
        assertEquals(0.0, g.weight("library.model.Dvd", "library.model.Item"), EPS);
    }

    @Test
    void miniTroisNoeudsDeuxAretesAucuneIsolee() throws IOException {
        CouplingGraph g = mini();
        assertEquals(3, g.nodes().size());
        assertEquals(2, g.edges().size());
        assertTrue(g.isolated().isEmpty());
        assertEquals(2.0 / 3, g.weight("demo.A", "demo.B"), EPS);
    }

    @Test
    void lExportCsvSeRechargeALIdentique(@TempDir Path tmp) throws IOException {
        CouplingGraph g = validation();
        Path csv = tmp.resolve("graphe.csv");
        GraphIO.writeCsv(g, csv);
        CouplingGraph relu = GraphIO.readCsv(csv);
        assertEquals(g, relu);
        assertEquals(g.isolated(), relu.isolated());
        assertEquals(37, relu.interArcs());
    }

    @Test
    void lExportDotContientTousLesNoeudsEtAretes(@TempDir Path tmp) throws IOException {
        Path dot = tmp.resolve("graphe.dot");
        GraphIO.writeDot(validation(), dot);
        String s = Files.readString(dot, StandardCharsets.UTF_8);
        assertTrue(s.startsWith("graph "));
        assertEquals(19, s.split(" -- ", -1).length - 1);
        assertEquals(15, s.lines().filter(l -> l.contains("[label=\"") && !l.contains(" -- ")).count());
    }

    @Test
    void unCsvMalFormeEstRefuse(@TempDir Path tmp) throws IOException {
        Path csv = tmp.resolve("mauvais.csv");
        Files.writeString(csv, "kind;a;b;r;interArcs;weight\nedge;x.A;x.B;deux;3;0.5\n",
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> GraphIO.readCsv(csv));
        Files.writeString(csv, "kind;a;b;r;interArcs;weight\nnode;x.A;;;;\nedge;x.A;x.Z;1;3;0.3\n",
                StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> GraphIO.readCsv(csv));   // nœud x.Z non déclaré
    }
}
