package hai913i.tp2.graph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import hai913i.tp2.coupling.ClassPair;
import hai913i.tp2.output.Names;

/**
 * Exports du graphe de couplage (étape A2) : un CSV que le programme sait
 * relire, et un fichier DOT pour Graphviz.
 *
 * <p><b>Format CSV</b> (séparateur {@code ;}, UTF-8, une ligne d'en-tête) :</p>
 * <pre>
 *   kind;a;b;r;interArcs;weight
 *   node;library.model.Dvd;;;;
 *   edge;library.model.Item;library.service.LoanService;4;37;0.108108
 * </pre>
 * <p>Les lignes {@code node} préservent les classes isolées, qui n'apparaîtraient
 * dans aucune ligne {@code edge}. Le poids décimal n'est là que pour la lecture
 * humaine : à la relecture, il est recalculé exactement comme r / interArcs.</p>
 */
public final class GraphIO {

    private static final String HEADER = "kind;a;b;r;interArcs;weight";

    private GraphIO() {
    }

    /**
     * Écrit le graphe au format CSV relisible.
     *
     * @param g    le graphe
     * @param file fichier de destination (écrasé s'il existe)
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void writeCsv(CouplingGraph g, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (String n : g.nodes()) {
            lines.add("node;" + n + ";;;;");
        }
        for (CouplingGraph.Edge e : g.edges()) {
            lines.add(String.format(Locale.ROOT, "edge;%s;%s;%d;%d;%.6f",
                    e.a(), e.b(), e.r(), g.interArcs(), e.weight()));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * Relit un graphe écrit par {@link #writeCsv(CouplingGraph, Path)}.
     *
     * @param file le fichier CSV
     * @return le graphe reconstruit
     * @throws IOException fichier absent, ligne mal formée, arête vers un nœud
     *                     non déclaré, ou valeurs de interArcs incohérentes
     */
    public static CouplingGraph readCsv(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("fichier introuvable : " + file.toAbsolutePath());
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).strip().equals(HEADER)) {
            throw new IOException(file.getFileName() + " : en-tête attendu « " + HEADER + " »");
        }
        List<String> nodes = new ArrayList<>();
        Set<String> known = new HashSet<>();
        List<String[]> edgeRows = new ArrayList<>();
        int interArcs = -1;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(";", -1);
            String where = file.getFileName() + " ligne " + (i + 1);
            if (f.length != 6) {
                throw new IOException(where + " : 6 colonnes attendues, " + f.length + " trouvées");
            }
            switch (f[0]) {
                case "node" -> {
                    if (f[1].isEmpty() || !known.add(f[1])) {
                        throw new IOException(where + " : nœud vide ou en double");
                    }
                    nodes.add(f[1]);
                }
                case "edge" -> {
                    int ia = parseInt(f[4], where);
                    if (interArcs != -1 && ia != interArcs) {
                        throw new IOException(where + " : interArcs incohérent");
                    }
                    interArcs = ia;
                    edgeRows.add(new String[] {f[1], f[2], f[3], where});
                }
                default -> throw new IOException(where + " : type de ligne inconnu « " + f[0] + " »");
            }
        }
        // les arêtes, triées par couple comme dans CouplingGraph.from
        Map<ClassPair, CouplingGraph.Edge> edges = new TreeMap<>();
        for (String[] row : edgeRows) {
            String a = row[0];
            String b = row[1];
            int r = parseInt(row[2], row[3]);
            if (!known.contains(a) || !known.contains(b) || a.compareTo(b) >= 0 || r <= 0) {
                throw new IOException(row[3] + " : arête invalide " + a + " – " + b);
            }
            edges.put(new ClassPair(a, b), new CouplingGraph.Edge(a, b, r, (double) r / interArcs));
        }
        nodes.sort(null);
        return new CouplingGraph(nodes, new ArrayList<>(edges.values()), Math.max(interArcs, 0));
    }

    private static int parseInt(String s, String where) throws IOException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IOException(where + " : entier attendu, « " + s + " » trouvé");
        }
    }

    /**
     * Écrit le graphe au format DOT (Graphviz), non orienté.
     *
     * <p>Les classes sont regroupées par paquetage ({@code subgraph cluster_…}),
     * ce qui prépare la comparaison modules / paquetages de l'étape B4. Chaque
     * arête porte r/|E_inter| ; son épaisseur est proportionnelle à r. Les classes
     * isolées sont grisées.</p>
     *
     * <pre>dot -Tpng graphe-couplage.dot -o graphe-couplage.png</pre>
     *
     * @param g    le graphe
     * @param file fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public static void writeDot(CouplingGraph g, Path file) throws IOException {
        Set<String> isolated = new HashSet<>(g.isolated());
        Map<String, List<String>> byPackage = new TreeMap<>();
        for (String n : g.nodes()) {
            byPackage.computeIfAbsent(Names.packageOf(n), k -> new ArrayList<>()).add(n);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("graph couplage {\n");
        sb.append("  rankdir=LR; nodesep=0.4; ranksep=1.0;\n");
        sb.append("  node [shape=box, style=\"rounded,filled\", fillcolor=\"#eef3fb\", fontname=\"Helvetica\"];\n");
        sb.append("  edge [fontname=\"Helvetica\", fontsize=10];\n");
        int i = 0;
        for (Map.Entry<String, List<String>> pkg : byPackage.entrySet()) {
            sb.append("  subgraph cluster_").append(i++).append(" {\n");
            sb.append("    label=").append(quote(pkg.getKey())).append("; style=dashed; color=gray;\n");
            for (String n : pkg.getValue()) {
                sb.append("    ").append(quote(n)).append(" [label=").append(quote(Names.simple(n)));
                if (isolated.contains(n)) {
                    sb.append(", fillcolor=\"#dddddd\", fontcolor=\"#666666\"");
                }
                sb.append("];\n");
            }
            sb.append("  }\n");
        }
        for (CouplingGraph.Edge e : g.edges()) {
            sb.append("  ").append(quote(e.a())).append(" -- ").append(quote(e.b()))
              .append(" [label=\"").append(e.r()).append('/').append(g.interArcs())
              .append("\", penwidth=").append(e.r()).append("];\n");
        }
        sb.append("}\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}
