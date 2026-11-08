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

/**
 * Tests de l'étage d'entrée (point de contrôle A0).
 *
 * <p>On part du micro jeu de données {@code facts-mini} (trois classes, cinq
 * méthodes, quatre arcs), dont toutes les valeurs attendues se calculent à la
 * main : une erreur invisible sur quinze classes saute aux yeux sur trois. Le
 * couplage, le regroupement et la coupe ont leurs propres classes de test.</p>
 *
 * <p>On vérifie aussi que la lecture refuse clairement un fichier qui ne suit
 * pas le format canonique (en-tête inattendu, guillemets) plutôt que de
 * produire des faits faux.</p>
 */
class FactsTest {

    /** Le micro jeu de données, copié dans les ressources de test. */
    private static Facts mini() throws IOException {
        return Facts.load(Path.of("src", "test", "resources"), "facts-mini");
    }

    @Test
    void chargeLesTroisClasses() throws IOException {
        Facts f = mini();
        assertEquals(3, f.classes().size());
        // classNames() est trié : c'est l'ordre qui numérote les groupes
        assertEquals(List.of("demo.A", "demo.B", "demo.C"), f.classNames());
    }

    @Test
    void chargeLesQuatreArcs() throws IOException {
        Facts f = mini();
        assertEquals(4, f.calls().size());
        // quatre arcs, mais cinq sites d'appel
        int sites = f.calls().stream().mapToInt(Facts.CallArc::sites).sum();
        assertEquals(5, sites);
    }

    @Test
    void connaitLaClasseDeChaqueMethode() throws IOException {
        Facts f = mini();
        assertEquals(5, f.methodOwners().size());
        assertEquals("demo.A", f.methodOwners().get("demo.A#f()"));
        assertEquals("demo.B", f.methodOwners().get("demo.B#h()"));
        assertTrue(f.methodOwners().containsKey("demo.C#j()"));
    }

    @Test
    void deduitLePrefixeDuDossier() throws IOException {
        Path dir = Path.of("src", "test", "resources");
        assertEquals("facts-mini", Facts.discoverPrefix(dir));
        // le chargement sans préfixe doit donner exactement le même jeu
        assertEquals(mini().classNames(), Facts.load(dir).classNames());
    }

    @Test
    void unDossierSansJeuDeDonneesEstSignale() {
        Path absent = Path.of("src", "test", "resources", "nexiste-pas");
        IOException e = assertThrows(IOException.class, () -> Facts.load(absent));
        assertTrue(e.getMessage().contains("introuvable"), e.getMessage());
    }

    @Test
    void unArcSurQuatreEstInterneAUneClasse() throws IOException {
        Facts f = mini();
        // les arcs intra-classe relèvent de la cohésion : l'étape A1 les écarte
        long intra = f.calls().stream()
                .filter(a -> a.callerClass().equals(a.calleeClass()))
                .count();
        assertEquals(1, intra);
        // il reste |E_inter| = 3, le dénominateur du couplage sur ce jeu
        assertEquals(3, f.calls().size() - intra);
    }
    // --------------------------------------------- lecture stricte du format CSV

    /** Écrit un jeu de trois fichiers dans un dossier temporaire. */
    private static Path jeu(Path dir, String classes, String methods, String calls) throws IOException {
        Files.writeString(dir.resolve("t-classes.csv"), classes, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("t-methods.csv"), methods, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("t-calls.csv"), calls, StandardCharsets.UTF_8);
        return dir;
    }

    private static final String CLASSES = "class;simpleName;package\r\ndemo.A;A;demo\r\n";
    private static final String METHODS = "method;owner\r\ndemo.A#f();demo.A\r\n";
    private static final String CALLS = "caller;callerClass;callee;calleeClass;sites\r\n";

    @Test
    void unEnTeteInattenduEstRefuse(@TempDir Path dir) throws IOException {
        // colonnes inversées : sans contrôle, les noms de méthodes deviendraient des classes
        jeu(dir, CLASSES, "owner;method\r\ndemo.A;demo.A#f()\r\n", CALLS);
        IOException e = assertThrows(IOException.class, () -> Facts.load(dir, "t"));
        assertTrue(e.getMessage().contains("format non reconnu"), e.getMessage());
    }

    @Test
    void leBomUtf8EnTeteDeFichierEstIgnore(@TempDir Path dir) throws IOException {
        jeu(dir, "\uFEFF" + CLASSES, METHODS, CALLS);
        Facts f = Facts.load(dir, "t");
        assertEquals(List.of("demo.A"), f.classNames());
    }

    @Test
    void unChampEntreGuillemetsEstRefuse(@TempDir Path dir) throws IOException {
        jeu(dir, "class;simpleName;package\r\n\"demo.A\";A;demo\r\n", METHODS, CALLS);
        IOException e = assertThrows(IOException.class, () -> Facts.load(dir, "t"));
        assertTrue(e.getMessage().contains("guillemet"), e.getMessage());
    }
}
