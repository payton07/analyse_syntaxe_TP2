package hai913i.tp2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import hai913i.tp2.clustering.AverageLinkClustering;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Merge;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.coupling.ClassPair;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.graph.CouplingGraph;
import hai913i.tp2.graph.GraphIO;
import hai913i.tp2.input.FactSource;
import hai913i.tp2.input.Facts;
import hai913i.tp2.output.Names;

/**
 * Point d'entrée du squelette : vérifie l'environnement et charge les faits.
 *
 * <p><b>Ce qu'il fait.</b> Il lit les arguments de la ligne de commande, charge
 * un jeu de faits au format canonique, et affiche les trois compteurs du
 * <b>point de contrôle A0</b> : nombre de classes, de méthodes, d'arcs du
 * graphe d'appel.</p>
 *
 * <p><b>Ce qu'il ne fait pas.</b> Tout le reste. Le couplage (A1), le graphe
 * pondéré (A2), le regroupement (B1), le dendrogramme (B2) et la coupe (B3)
 * sont l'objet du TP : leurs emplacements sont marqués {@code TODO} ci-dessous,
 * et rien n'en est fourni.</p>
 *
 * <pre>
 *   mvn clean package
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS --cp 0.025
 * </pre>
 *
 * <p>Le dossier passé en argument est celui qui contient les trois fichiers
 * {@code ...-classes.csv}, {@code ...-methods.csv} et {@code ...-calls.csv}.
 * Le préfixe en est déduit : votre outil n'a donc aucune raison de connaître le
 * nom du projet de validation, et il fonctionnera tel quel sur un autre jeu de
 * faits..</p>
 *
 * <p><b>Codes de sortie</b> : 0 si les faits sont chargés, 1 si les arguments
 * sont invalides, 2 si les fichiers sont illisibles. Gardez cette convention
 * dans votre propre outil : elle fait partie du critère de reproductibilité.</p>
 */
public final class Main {

    /**
     * Sortie standard forcée en UTF-8.
     *
     * <p>Sans cela, une console Windows configurée sur une autre page de codes
     * afficherait les accents de travers. Le jeu de données, les sources et
     * cette sortie sont tous en UTF-8.</p>
     */
    private static final PrintStream OUT =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** Sortie d'erreur, forcée en UTF-8 pour la même raison. */
    private static final PrintStream ERR =
            new PrintStream(System.err, true, StandardCharsets.UTF_8);

    /** Seuil de coupe par défaut, repris du point de contrôle B3 du sujet. */
    private static final double CP_DEFAUT = 0.025;

    /** Dossier des exports par défaut (graphe, dendrogramme, modules). */
    private static final String OUT_DEFAUT = "sorties";

    private Main() {
    }

    /**
     * Charge les faits et affiche les compteurs de l'étape A0.
     *
     * @param args {@code <dossier-des-faits> [--cp <valeur>] [--prefix <préfixe>]}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage("chemin du dossier des faits manquant");
            return;
        }
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        String prefix = null;
        Path out = Path.of(OUT_DEFAUT);
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i]) && i + 1 < args.length) {
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    usage("valeur de --cp invalide : " + args[i]);
                    return;
                }
            } else if ("--prefix".equals(args[i]) && i + 1 < args.length) {
                prefix = args[++i];
            } else if ("--out".equals(args[i]) && i + 1 < args.length) {
                out = Path.of(args[++i]);
            } else {
                usage("argument inconnu : " + args[i]);
                return;
            }
        }
        if (cp < 0.0) {
            usage("--cp doit être positif ou nul");
            return;
        }

        // A0 — le reste du programme ne voit que l'interface FactSource
        FactSource facts;
        try {
            if (prefix == null) {
                prefix = Facts.discoverPrefix(dir);
            }
            facts = Facts.load(dir, prefix);
        } catch (IOException e) {
            ERR.println("Lecture des faits impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        OUT.println("Dossier des faits   : " + dir.toAbsolutePath());
        OUT.println("Jeu de données      : " + prefix);
        OUT.printf(Locale.ROOT, "Classes             : %d%n",
                facts.classNames().size());
        OUT.printf(Locale.ROOT, "Méthodes            : %d%n",
                facts.methodOwners().size());
        OUT.printf(Locale.ROOT, "Arcs du graphe      : %d%n",
                facts.calls().size());
        OUT.printf(Locale.ROOT, "Seuil CP retenu     : %.4f%n", cp);

        // A1 — couplage entre classes
        Coupling coupling;
        try {
            coupling = Coupling.from(facts);
        } catch (IllegalArgumentException e) {
            ERR.println("Faits incohérents : " + e.getMessage());
            System.exit(2);
            return;
        }
        printCoupling(coupling);

        // A2 — graphe de couplage pondéré, exporté puis relu
        CouplingGraph graph = CouplingGraph.from(coupling);
        try {
            Files.createDirectories(out);
            Path csv = out.resolve("graphe-couplage.csv");
            Path dot = out.resolve("graphe-couplage.dot");
            GraphIO.writeCsv(graph, csv);
            GraphIO.writeDot(graph, dot);
            CouplingGraph reloaded = GraphIO.readCsv(csv);
            printGraph(graph, reloaded.equals(graph), csv, dot);
        } catch (IOException e) {
            ERR.println("Écriture des exports impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        if (facts.classNames().isEmpty()) {
            ERR.println("Aucune classe dans le jeu de données : rien à regrouper.");
            System.exit(2);
            return;
        }

        // B1 — regroupement ascendant, lien moyen ; il ne reçoit que la matrice
        Dendrogram dendrogram = AverageLinkClustering.run(coupling.classes(),
                Similarity.fromMatrix(coupling.matrix()));
        printMerges(dendrogram);

        // TODO étape B3 : couper l'arbre sous les trois contraintes, puis
        //                 vérifier la contrainte M/2 et signaler sa violation.
        OUT.println();
        OUT.println("L'étape B3 reste à écrire.");
    }

    /**
     * Affiche la suite des fusions de l'étape B1 et le contrôle de monotonie.
     *
     * @param d le dendrogramme
     */
    private static void printMerges(Dendrogram d) {
        OUT.println();
        OUT.println("== B1 · Regroupement ascendant (lien moyen) ==");
        OUT.printf(Locale.ROOT, "Fusions               : %d%n", d.merges().size());
        OUT.printf(Locale.ROOT, "  %5s  %-8s  %s%n", "étape", "S", "groupe A  +  groupe B  →  nouveau");
        boolean monotone = true;
        double previous = Double.POSITIVE_INFINITY;
        for (Merge m : d.merges()) {
            OUT.printf(Locale.ROOT, "  %5d  %.4f    G%d %s  +  G%d %s  →  G%d%n",
                    m.step(), m.similarity(),
                    m.a().id(), Names.braces(d.labelsOf(m.a())),
                    m.b().id(), Names.braces(d.labelsOf(m.b())),
                    m.result().id());
            if (m.similarity() > previous + AverageLinkClustering.EPS) {
                monotone = false;
            }
            previous = m.similarity();
        }
        OUT.println("Similarités           : "
                + (monotone ? "ne remontent jamais (contrôle OK)" : "ANOMALIE : une similarité remonte"));
    }

    /**
     * Affiche le résultat de l'étape A2 : taille du graphe, cinq arêtes les plus
     * lourdes, classes isolées, fichiers écrits et contrôle de relecture.
     *
     * @param g          le graphe
     * @param reloadedOk vrai si le CSV relu redonne exactement le même graphe
     * @param csv        export CSV écrit
     * @param dot        export DOT écrit
     */
    private static void printGraph(CouplingGraph g, boolean reloadedOk, Path csv, Path dot) {
        OUT.println();
        OUT.println("== A2 · Graphe de couplage pondéré ==");
        OUT.printf(Locale.ROOT, "Nœuds                 : %d%n", g.nodes().size());
        OUT.printf(Locale.ROOT, "Arêtes                : %d%n", g.edges().size());
        OUT.println("Cinq arêtes les plus lourdes :");
        for (CouplingGraph.Edge e : g.topEdges(5)) {
            OUT.printf(Locale.ROOT, "  %-34s %-34s %7s  %.4f%n", e.a(), e.b(),
                    e.r() + "/" + g.interArcs(), e.weight());
        }
        OUT.println("Classes isolées       : "
                + (g.isolated().isEmpty() ? "aucune" : String.join(", ", g.isolated())));
        OUT.println("Export CSV            : " + csv);
        OUT.println("Export DOT            : " + dot);
        OUT.println("Rechargement du CSV   : "
                + (reloadedOk ? "identique (contrôle OK)" : "ANOMALIE : graphe relu différent"));
    }

    /**
     * Affiche le résultat de l'étape A1 : arcs écartés, dénominateur, couples
     * par couplage décroissant, et contrôle de la somme.
     *
     * @param c le couplage calculé
     */
    private static void printCoupling(Coupling c) {
        OUT.println();
        OUT.println("== A1 · Couplage entre classes ==");
        OUT.printf(Locale.ROOT, "Arcs internes écartés : %d%n", c.internalArcs());
        OUT.printf(Locale.ROOT, "|E_inter|             : %d%n", c.interArcs());
        OUT.printf(Locale.ROOT, "Couples non nuls      : %d / %d%n",
                c.nonZeroPairs().size(), c.possiblePairs());
        OUT.println();
        OUT.printf(Locale.ROOT, "  %-34s %-34s %3s  %7s  %s%n",
                "classe A", "classe B", "r", "r/|E|", "C(A,B)");
        for (Map.Entry<ClassPair, Integer> e : c.byDecreasingCoupling()) {
            ClassPair p = e.getKey();
            OUT.printf(Locale.ROOT, "  %-34s %-34s %3d  %7s  %.4f%n",
                    p.first(), p.second(), e.getValue(),
                    e.getValue() + "/" + c.interArcs(),
                    c.c(p.first(), p.second()));
        }
        double total = c.total();
        boolean ok = c.interArcs() == 0 || Math.abs(total - 1.0) < 1e-9;
        OUT.println();
        OUT.printf(Locale.ROOT, "Somme des couplages   : %.4f  %s%n",
                total, ok ? "(contrôle OK)" : "(ANOMALIE : devrait valoir 1)");
    }

    /**
     * Affiche un message d'erreur, le mode d'emploi, et sort avec le code 1.
     *
     * @param message cause de l'échec, en une ligne
     */
    private static void usage(String message) {
        ERR.println("Erreur : " + message);
        ERR.println();
        ERR.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits>"
                + " [--cp <valeur>] [--prefix <préfixe>] [--out <dossier>]");
        ERR.println();
        ERR.println("  <dossier-des-faits>  dossier contenant les trois fichiers");
        ERR.println("                       <préfixe>-classes.csv,");
        ERR.println("                       <préfixe>-methods.csv et");
        ERR.println("                       <préfixe>-calls.csv");
        ERR.println("  --cp <valeur>        seuil de couplage interne moyen"
                + " (défaut : " + CP_DEFAUT + ")");
        ERR.println("  --prefix <préfixe>   force le préfixe ; par défaut il est");
        ERR.println("                       déduit du dossier");
        ERR.println("  --out <dossier>      dossier des exports (défaut : "
                + OUT_DEFAUT + ")");
        System.exit(1);
    }
}
