package hai913i.tp2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Linkage;
import hai913i.tp2.clustering.Merge;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.clustering.TreeComparison;
import hai913i.tp2.coupling.ClassPair;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.cut.TreeCut;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.graph.CouplingGraph;
import hai913i.tp2.graph.GraphIO;
import hai913i.tp2.input.FactSource;
import hai913i.tp2.input.Facts;
import hai913i.tp2.input.Inheritance;
import hai913i.tp2.output.DendrogramExport;
import hai913i.tp2.output.ModulesExport;
import hai913i.tp2.output.Names;

/**
 * Point d'entrée : enchaîne toute la chaîne du TP 2, des faits aux modules.
 *
 * <p><b>Rôle.</b> On lit la ligne de commande, on appelle chaque étape dans
 * l'ordre, on affiche les points de contrôle et on écrit les exports. On ne
 * fait aucun calcul ici : chaque étape vit dans son paquetage.</p>
 * <ol>
 *   <li>A0 — {@code input} : les faits, derrière l'interface {@link FactSource} ;</li>
 *   <li>A1 — {@code coupling} : C(A, B) = r(A, B) / |E_inter| ;</li>
 *   <li>A2 — {@code graph} : graphe pondéré, exports CSV (relu) et DOT ;</li>
 *   <li>B1/B2 — {@code clustering} : regroupement ascendant, lien moyen,
 *       départage des égalités ; {@code output} : arbre indenté, fusions, DOT ;</li>
 *   <li>B3 — {@code cut} : coupe au(x) seuil(s) CP, contrainte M/2 signalée.</li>
 * </ol>
 *
 * <pre>
 *   mvn clean package
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS --cp 0.025
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS --cp 0.015,0.025,0.045
 * </pre>
 *
 * <p>Le dossier passé en argument est celui qui contient les trois fichiers
 * {@code ...-classes.csv}, {@code ...-methods.csv} et {@code ...-calls.csv}.
 * On en déduit le préfixe : l'outil n'a donc pas besoin de connaître le nom
 * du projet de validation, et il fonctionne tel quel sur un autre jeu de
 * faits.</p>
 *
 * <p><b>Codes de sortie</b> : 0 si la chaîne s'est exécutée (y compris quand
 * une coupe viole M/2 : c'est un résultat, pas une panne), 1 si les arguments
 * sont invalides, 2 si les fichiers sont illisibles, incohérents ou si les
 * exports ne peuvent pas être écrits.</p>
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
     * Exécute la chaîne complète, de A0 à B3.
     *
     * @param args {@code <dossier-des-faits> [--cp <v1,v2,…>] [--prefix <préfixe>] [--out <dossier>]}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage("chemin du dossier des faits manquant");
            return;
        }
        Path dir = Path.of(args[0]);
        List<Double> cps = List.of(CP_DEFAUT);
        String prefix = null;
        Path out = Path.of(OUT_DEFAUT);
        Linkage linkage = Linkage.MOYEN;
        boolean heritage = false;
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i]) && i + 1 < args.length) {
                cps = parseCps(args[++i]);
                if (cps == null) {
                    return;                                  // usage() déjà appelé
                }
            } else if ("--prefix".equals(args[i]) && i + 1 < args.length) {
                prefix = args[++i];
            } else if ("--out".equals(args[i]) && i + 1 < args.length) {
                out = Path.of(args[++i]);
            } else if ("--heritage".equals(args[i])) {
                heritage = true;
            } else if ("--lien".equals(args[i]) && i + 1 < args.length) {
                linkage = Linkage.parse(args[++i]);
                if (linkage == null) {
                    usage("valeur de --lien invalide : « " + args[i] + " » (moyen ou simple)");
                    return;
                }
            } else {
                usage("argument inconnu : " + args[i]);
                return;
            }
        }

        // A0 — on charge les faits ; la suite ne voit que l'interface FactSource
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
        // bonus C2 : on lit l'héritage seulement si on le demande, le JSON est facultatif
        List<Inheritance.Link> links = List.of();
        try {
            if (heritage) {
                links = Inheritance.load(Inheritance.jsonFile(dir, prefix));
            }
        } catch (IOException e) {
            ERR.println("Lecture de l'héritage impossible : " + e.getMessage());
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
        OUT.println("Seuil(s) CP         : " + String.join(", ",
                cps.stream().map(ModulesExport::cpLabel).toList()));

        // A1 — on calcule le couplage entre classes
        Coupling coupling;
        try {
            coupling = Coupling.from(facts, links);
        } catch (IllegalArgumentException e) {
            ERR.println("Faits incohérents : " + e.getMessage());
            System.exit(2);
            return;
        }
        printCoupling(coupling);

        // A2 — on construit le graphe pondéré, on l'exporte, puis on relit le CSV pour vérifier
        CouplingGraph graph = CouplingGraph.from(coupling);
        try {
            Files.createDirectories(out);
            String graphSuffix = heritage ? "-heritage" : "";
            Path csv = out.resolve("graphe-couplage" + graphSuffix + ".csv");
            Path dot = out.resolve("graphe-couplage" + graphSuffix + ".dot");
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

        // B1 — on regroupe les classes ; le regroupement ne reçoit que la matrice
        Similarity sim = Similarity.fromMatrix(coupling.matrix());
        Dendrogram dendrogram = HierarchicalClustering.run(coupling.classes(), sim, linkage);
        printMerges(dendrogram, linkage);
        if (linkage != Linkage.MOYEN) {
            // bonus C1 : on reconstruit l'arbre de référence pour comparer
            printComparison(HierarchicalClustering.run(coupling.classes(), sim, Linkage.MOYEN),
                    dendrogram, linkage);
        }
        String suffix = (linkage == Linkage.MOYEN ? "" : "-" + linkage.label())
                + (heritage ? "-heritage" : "");

        // B2 — on écrit le dendrogramme sous ses trois formes
        try {
            List<Path> files = DendrogramExport.writeAll(dendrogram, out, suffix);
            OUT.println();
            OUT.println("== B2 · Dendrogramme ==");
            OUT.printf(Locale.ROOT, "Feuilles / nœuds internes : %d / %d%n",
                    coupling.classes().size(), dendrogram.merges().size());
            for (Path f : files) {
                OUT.println("Export                : " + f);
            }
        } catch (IOException e) {
            ERR.println("Écriture des exports impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        // B3 — on coupe l'arbre pour chaque seuil demandé
        List<CutResult> cuts = new ArrayList<>();
        try {
            for (double cp : cps) {
                CutResult r = TreeCut.cut(dendrogram, sim, cp);
                cuts.add(r);
                List<Path> files = ModulesExport.write(r, dendrogram, graph, out, suffix);
                printCut(r, dendrogram, files);
            }
        } catch (IOException e) {
            ERR.println("Écriture des exports impossible : " + e.getMessage());
            System.exit(2);
            return;
        }
        if (cuts.size() > 1) {
            printCutSummary(cuts);
        }
        if (heritage) {
            // bonus C2 : on refait la chaîne sans l'héritage, avec le même lien, pour comparer
            Coupling plain = Coupling.from(facts);
            Similarity plainSim = Similarity.fromMatrix(plain.matrix());
            Dendrogram plainTree = HierarchicalClustering.run(plain.classes(), plainSim, linkage);
            List<CutResult> plainCuts = new ArrayList<>();
            for (double cp : cps) {
                plainCuts.add(TreeCut.cut(plainTree, plainSim, cp));
            }
            printHeritageComparison(plainCuts, plainTree, cuts, dendrogram);
        }
    }

    /**
     * Lit la valeur de {@code --cp} : un seuil, ou plusieurs séparés par des virgules.
     *
     * @param text la valeur passée sur la ligne de commande
     * @return les seuils, dans l'ordre donné ; {@code null} après usage() si invalide
     */
    private static List<Double> parseCps(String text) {
        List<Double> out = new ArrayList<>();
        for (String part : text.split(",")) {
            double v;
            try {
                v = Double.parseDouble(part.trim());
            } catch (NumberFormatException e) {
                usage("valeur de --cp invalide : « " + part.trim() + " »");
                return null;
            }
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) {
                usage("--cp doit être un nombre positif ou nul : « " + part.trim() + " »");
                return null;
            }
            out.add(v);
        }
        return out;
    }

    /**
     * Affiche une coupe de l'étape B3 : modules, taille, couplage interne moyen,
     * classes, et verdict sur la contrainte M/2.
     *
     * @param r     la coupe
     * @param d     le dendrogramme
     * @param files les exports écrits
     */
    private static void printCut(CutResult r, Dendrogram d, List<Path> files) {
        OUT.println();
        OUT.println("== B3 · Modules à CP = " + ModulesExport.cpLabel(r.cp()) + " ==");
        OUT.printf(Locale.ROOT, "  %-4s %-5s %6s  %-8s %s%n", "", "nœud", "taille", "C̄(G)", "classes");
        int k = 1;
        for (CutResult.Module m : r.modules()) {
            OUT.printf(Locale.ROOT, "  M%-3d G%-4d %6d  %-8s %s%n", k++, m.cluster().id(), m.size(),
                    Double.isNaN(m.internalCoupling()) ? "—" : String.format(Locale.ROOT, "%.4f", m.internalCoupling()),
                    Names.braces(d.labelsOf(m.cluster())));
        }
        OUT.printf(Locale.ROOT, "Modules               : %d (plafond M/2 = %d)%n",
                r.modules().size(), r.maxModules());
        OUT.println("Contrainte M/2        : " + (r.respectsMaxModules()
                ? "respectée"
                : "VIOLÉE — coupe rejetée : avec ce seuil, les contraintes sont incompatibles"));
        for (Path f : files) {
            OUT.println("Export                : " + f);
        }
    }

    /**
     * Tableau récapitulatif quand plusieurs seuils ont été demandés.
     *
     * @param cuts les coupes, dans l'ordre des seuils
     */
    private static void printCutSummary(List<CutResult> cuts) {
        OUT.println();
        OUT.println("== B3 · Récapitulatif des seuils ==");
        OUT.printf(Locale.ROOT, "  %-8s %8s %13s  %s%n", "CP", "modules", "plus gros", "contrainte M/2");
        for (CutResult r : cuts) {
            OUT.printf(Locale.ROOT, "  %-8s %8d %13d  %s%n", ModulesExport.cpLabel(r.cp()),
                    r.modules().size(), r.largestModuleSize(),
                    r.respectsMaxModules() ? "respectée" : "VIOLÉE (rejetée)");
        }
    }

    /**
     * Affiche la suite des fusions de l'étape B1 et le contrôle de monotonie.
     *
     * @param d le dendrogramme
     */
    private static void printMerges(Dendrogram d, Linkage linkage) {
        OUT.println();
        OUT.println("== B1 · Regroupement ascendant (lien " + linkage.label() + ") ==");
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
            if (m.similarity() > previous + HierarchicalClustering.EPS) {
                monotone = false;
            }
            previous = m.similarity();
        }
        OUT.println("Similarités           : "
                + (monotone ? "ne remontent jamais (contrôle OK)" : "ANOMALIE : une similarité remonte"));
    }

    /**
     * Bonus C2 : pour chaque seuil, les modules sans puis avec l'héritage.
     *
     * @param plainCuts coupes sur la relation imposée (appels seuls)
     * @param plainTree arbre correspondant
     * @param cuts      coupes avec l'héritage
     * @param tree      arbre correspondant
     */
    private static void printHeritageComparison(List<CutResult> plainCuts, Dendrogram plainTree,
                                                List<CutResult> cuts, Dendrogram tree) {
        OUT.println();
        OUT.println("== C2 · Modules sans et avec l'héritage ==");
        for (int k = 0; k < cuts.size(); k++) {
            CutResult a = plainCuts.get(k);
            CutResult b = cuts.get(k);
            OUT.println("CP = " + ModulesExport.cpLabel(a.cp()));
            printModuleLine("  appels seuls      ", a, plainTree);
            printModuleLine("  appels + héritage ", b, tree);
        }
    }

    private static void printModuleLine(String title, CutResult r, Dendrogram d) {
        List<String> mods = new ArrayList<>();
        for (CutResult.Module m : r.modules()) {
            mods.add(Names.braces(d.labelsOf(m.cluster())));
        }
        OUT.printf(Locale.ROOT, "%s: %d modules, M/2 %s%n", title, r.modules().size(),
                r.respectsMaxModules() ? "respectée" : "VIOLÉE");
        for (String m : mods) {
            OUT.println("      " + m);
        }
    }

    /**
     * Bonus C1 : affiche côte à côte les fusions des deux arbres et la première
     * étape où ils divergent.
     *
     * @param reference l'arbre au lien moyen
     * @param other     l'arbre construit avec l'autre critère
     * @param linkage   cet autre critère
     */
    private static void printComparison(Dendrogram reference, Dendrogram other, Linkage linkage) {
        OUT.println();
        OUT.println("== C1 · Comparaison : lien moyen et lien " + linkage.label() + " ==");
        OUT.printf(Locale.ROOT, "  %5s  %-8s %-46s  %-8s %s%n", "étape", "S moyen", "fusion (lien moyen)",
                "S " + linkage.label(), "fusion (lien " + linkage.label() + ")");
        int first = TreeComparison.firstDivergence(reference, other);
        for (int k = 0; k < reference.merges().size(); k++) {
            Merge a = reference.merges().get(k);
            Merge b = other.merges().get(k);
            OUT.printf(Locale.ROOT, "  %5d  %.4f   %-46s  %.4f   %s%s%n", a.step(),
                    a.similarity(), shortMerge(reference, a), b.similarity(), shortMerge(other, b),
                    first != 0 && a.step() >= first && !TreeComparison.sameMerge(a, b) ? "   ≠" : "");
        }
        OUT.println("Première divergence   : "
                + (first == 0 ? "aucune, les deux arbres sont identiques" : "étape " + first));
    }

    /** Une fusion en une ligne courte : « {Item, LoanService} + {Loan} ». */
    private static String shortMerge(Dendrogram d, Merge m) {
        String a = Names.braces(d.labelsOf(m.a()));
        String b = Names.braces(d.labelsOf(m.b()));
        String s = a + " + " + b;
        if (s.length() <= 46) {
            return s;
        }
        // trop long : on montre le plus petit groupe et la taille de l'autre
        boolean aSmaller = m.a().size() <= m.b().size();
        return (aSmaller ? a : b) + " + groupe de " + (aSmaller ? m.b() : m.a()).size() + " classes";
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
        if (c.inheritanceLinks() > 0 || c.ignoredInheritanceLinks() > 0) {
            OUT.printf(Locale.ROOT, "Liens d'héritage      : %d comptés, %d écartés (types hors du projet)%n",
                    c.inheritanceLinks(), c.ignoredInheritanceLinks());
            OUT.printf(Locale.ROOT, "|E_inter|             : %d (%d appels + %d liens d'héritage)%n",
                    c.interArcs(), c.interArcs() - c.inheritanceLinks(), c.inheritanceLinks());
        } else {
            OUT.printf(Locale.ROOT, "|E_inter|             : %d%n", c.interArcs());
        }
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
                + " [--cp <v1,v2,…>] [--prefix <préfixe>] [--out <dossier>]"
                + " [--lien moyen|simple] [--heritage]");
        ERR.println();
        ERR.println("  <dossier-des-faits>  dossier contenant les trois fichiers");
        ERR.println("                       <préfixe>-classes.csv,");
        ERR.println("                       <préfixe>-methods.csv et");
        ERR.println("                       <préfixe>-calls.csv");
        ERR.println("  --cp <v1,v2,…>       seuil(s) de couplage interne moyen, séparés");
        ERR.println("                       par des virgules (défaut : " + CP_DEFAUT + ")");
        ERR.println("  --prefix <préfixe>   force le préfixe ; par défaut il est");
        ERR.println("                       déduit du dossier");
        ERR.println("  --out <dossier>      dossier des exports (défaut : "
                + OUT_DEFAUT + ")");
        ERR.println("  --lien moyen|simple  critère de liaison (défaut : moyen) ; avec");
        ERR.println("                       simple, compare aussi les deux arbres (bonus C1)");
        ERR.println("  --heritage           ajoute l'héritage lu dans <préfixe>.json et");
        ERR.println("                       compare les modules obtenus (bonus C2)");
        System.exit(1);
    }
}
