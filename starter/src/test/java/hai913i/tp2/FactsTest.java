package hai913i.tp2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test de fumée du squelette.
 *
 * <p><b>Ce qu'il vérifie.</b> Uniquement l'étage d'entrée (le point de
 * contrôle A0). Il porte sur le micro jeu de données {@code facts-mini} (trois
 * classes, cinq méthodes, quatre arcs), dont toutes les valeurs attendues sont
 * calculées à la main dans {@code examples/README.txt} et donc vérifiables sans
 * exécuter quoi que ce soit.</p>
 *
 * <p><b>Pourquoi ce jeu-là.</b> Une erreur de couplage ou de moyenne est
 * invisible sur quinze classes et saute aux yeux sur trois. Gardez ce test et
 * <b>complétez-le</b> au fur et à mesure : la somme des couplages vaut 1, les
 * deux fusions valent 0,6667 puis 0,1667, la coupe à CP = 0,20 donne un module
 * et celle à CP = 0,50 en donne deux, donc viole M/2. C'est le moyen le plus
 * rapide de savoir si votre chaîne est juste.</p>
 *
 * <p><b>Ce qu'il ne vérifie pas.</b> Le couplage, le regroupement et la coupe :
 * ils ne sont pas fournis, c'est à vous de les écrire et de les tester.</p>
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
}
