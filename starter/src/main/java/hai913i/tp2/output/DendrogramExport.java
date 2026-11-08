package hai913i.tp2.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Merge;

/**
 * Restitution du dendrogramme (étape B2), lisible sans exécuter le code.
 *
 * <p>Trois formats complémentaires (annexe § 4.2) :</p>
 * <ul>
 *   <li><b>arbre indenté</b> — une ligne par nœud, pour lire le résultat d'un coup d'œil ;</li>
 *   <li><b>liste des fusions</b> (CSV) — une ligne par fusion, pour comparer deux
 *       exécutions et écrire des tests ;</li>
 *   <li><b>DOT</b> — l'image ({@code dot -Tpng dendrogramme.dot -o dendrogramme.png}).
 *       L'ordonnée est l'<em>ordre</em> des fusions (1 à M − 1) et la similarité
 *       réelle est écrite en regard, comme sur la diapo 33 du CM 03 : avec des
 *       similarités entre 0 et 0,108, une échelle proportionnelle serait illisible.</li>
 * </ul>
 */
public final class DendrogramExport {

    /** Nom des trois fichiers écrits par {@link #writeAll(Dendrogram, Path)}. */
    public static final String TREE_FILE = "dendrogramme.txt";
    public static final String MERGES_FILE = "fusions.csv";
    public static final String DOT_FILE = "dendrogramme.dot";

    private DendrogramExport() {
    }

    /**
     * Écrit les trois exports dans un dossier.
     *
     * @param d   le dendrogramme
     * @param dir dossier de destination (doit exister)
     * @return les trois fichiers écrits
     * @throws IOException en cas d'erreur d'écriture
     */
    public static List<Path> writeAll(Dendrogram d, Path dir) throws IOException {
        return writeAll(d, dir, "");
    }

    /**
     * Écrit les trois exports avec un suffixe dans le nom des fichiers. On s'en
     * sert pour les variantes des bonus, sans écraser les sorties de référence :
     * {@code -simple} donne {@code dendrogramme-simple.txt}, etc.
     *
     * @param d      le dendrogramme
     * @param dir    dossier de destination (doit exister)
     * @param suffix suffixe ajouté avant l'extension, vide pour les noms de base
     * @return les trois fichiers écrits
     * @throws IOException en cas d'erreur d'écriture
     */
    public static List<Path> writeAll(Dendrogram d, Path dir, String suffix) throws IOException {
        Path tree = dir.resolve(withSuffix(TREE_FILE, suffix));
        Path merges = dir.resolve(withSuffix(MERGES_FILE, suffix));
        Path dot = dir.resolve(withSuffix(DOT_FILE, suffix));
        Files.writeString(tree, indentedTree(d), StandardCharsets.UTF_8);
        Files.writeString(merges, mergesCsv(d), StandardCharsets.UTF_8);
        Files.writeString(dot, dot(d), StandardCharsets.UTF_8);
        return List.of(tree, merges, dot);
    }

    /** On insère le suffixe juste avant l'extension : fusions.csv → fusions-simple.csv. */
    private static String withSuffix(String file, String suffix) {
        int dot = file.lastIndexOf('.');
        return file.substring(0, dot) + suffix + file.substring(dot);
    }

    // ------------------------------------------------------------ arbre indenté

    /**
     * @param d le dendrogramme
     * @return l'arbre indenté, racine en haut
     */
    public static String indentedTree(Dendrogram d) {
        StringBuilder sb = new StringBuilder();
        appendTree(d, d.root(), "", sb);
        return sb.toString();
    }

    private static void appendTree(Dendrogram d, Cluster c, String indent, StringBuilder sb) {
        if (c.isLeaf()) {
            sb.append(indent).append("- ").append(Names.simple(d.labels().get(c.id())))
              .append("  (#").append(c.id()).append(")\n");
            return;
        }
        sb.append(String.format(Locale.ROOT, "%s+ G%d  fusion %d à %.4f  %d classes %s%n",
                indent, c.id(), c.step(), c.similarity(), c.size(),
                Names.braces(d.labelsOf(c))));
        appendTree(d, c.left(), indent + "    ", sb);
        appendTree(d, c.right(), indent + "    ", sb);
    }

    // --------------------------------------------------------- liste des fusions

    /**
     * @param d le dendrogramme
     * @return la liste des fusions au format CSV (séparateur {@code ;})
     */
    public static String mergesCsv(Dendrogram d) {
        StringBuilder sb = new StringBuilder();
        sb.append("etape;similarite;idA;idB;nouveau;tailleA;tailleB;membresA;membresB\n");
        for (Merge m : d.merges()) {
            sb.append(String.format(Locale.ROOT, "%d;%.6f;%d;%d;%d;%d;%d;%s;%s%n",
                    m.step(), m.similarity(), m.a().id(), m.b().id(), m.result().id(),
                    m.a().size(), m.b().size(),
                    shortList(d, m.a()), shortList(d, m.b())));
        }
        return sb.toString();
    }

    private static String shortList(Dendrogram d, Cluster c) {
        List<String> s = new ArrayList<>();
        for (String q : d.labelsOf(c)) {
            s.add(Names.simple(q));
        }
        return String.join(",", s);
    }

    // ---------------------------------------------------------------------- DOT

    /**
     * @param d le dendrogramme
     * @return le dendrogramme au format DOT, ordonnée = ordre des fusions
     */
    public static String dot(Dendrogram d) {
        int steps = d.merges().size();
        StringBuilder sb = new StringBuilder();
        sb.append("digraph dendrogramme {\n");
        sb.append("  rankdir=BT; ranksep=0.25; nodesep=0.15;\n");
        sb.append("  node [fontname=\"Helvetica\", fontsize=10];\n");
        sb.append("  edge [dir=none];\n\n");

        // on trace un axe vertical : une graduation par fusion, avec sa similarité en regard
        sb.append("  // axe : ordre des fusions et similarité\n");
        sb.append("  node [shape=plaintext, fontcolor=\"#555555\"];\n");
        sb.append("  axe0 [label=\"classes\"];\n");
        for (Merge m : d.merges()) {
            sb.append(String.format(Locale.ROOT, "  axe%d [label=\"fusion %d : %.4f\"];%n",
                    m.step(), m.step(), m.similarity()));
        }
        if (steps > 0) {
            sb.append("  axe0");
            for (int k = 1; k <= steps; k++) {
                sb.append(" -> axe").append(k);
            }
            sb.append(" [style=invis];\n");
        }

        // on place les feuilles dans l'ordre de l'arbre pour éviter les croisements
        sb.append("\n  // feuilles\n");
        sb.append("  node [shape=box, style=\"rounded,filled\", fillcolor=\"#eef3fb\", fontcolor=black];\n");
        List<Cluster> leaves = new ArrayList<>();
        collectLeaves(d.root(), leaves);
        StringBuilder sameRank = new StringBuilder("  { rank=same; axe0;");
        for (Cluster l : leaves) {
            sb.append("  G").append(l.id()).append(" [label=\"")
              .append(Names.simple(d.labels().get(l.id()))).append("\"];\n");
            sameRank.append(" G").append(l.id()).append(';');
        }
        sb.append(sameRank).append(" }\n");

        // on met chaque nœud interne au rang de sa fusion
        sb.append("\n  // fusions\n");
        sb.append("  node [shape=ellipse, style=filled, fillcolor=\"#fde8e8\", fontsize=9];\n");
        for (Merge m : d.merges()) {
            Cluster c = m.result();
            sb.append(String.format(Locale.ROOT,
                    "  G%d [label=\"G%d\\n%.4f\"]; { rank=same; axe%d; G%d; }%n",
                    c.id(), c.id(), c.similarity(), m.step(), c.id()));
            sb.append("  G").append(m.a().id()).append(" -> G").append(c.id()).append(";\n");
            sb.append("  G").append(m.b().id()).append(" -> G").append(c.id()).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void collectLeaves(Cluster c, List<Cluster> out) {
        if (c.isLeaf()) {
            out.add(c);
        } else {
            collectLeaves(c.left(), out);
            collectLeaves(c.right(), out);
        }
    }
}
