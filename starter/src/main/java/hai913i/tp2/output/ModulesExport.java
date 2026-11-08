package hai913i.tp2.output;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.graph.CouplingGraph;

/**
 * Restitution des modules d'une coupe (étape B3).
 *
 * <ul>
 *   <li>{@code modules-cp<CP>.csv} — une ligne par module : numéro, nœud de
 *       l'arbre, taille, couplage interne moyen, classes ;</li>
 *   <li>{@code modules-cp<CP>.dot} — le graphe de couplage où chaque <b>cadre est
 *       un module</b> et chaque <b>couleur un paquetage</b> réel : on voit d'un coup
 *       d'œil où modules et paquetages coïncident ou divergent (étape B4, point 1).
 *       Arêtes internes à un module en noir, arêtes entre modules en gris pointillé.</li>
 * </ul>
 */
public final class ModulesExport {

    /** Couleurs de remplissage, attribuées aux paquetages par ordre alphabétique. */
    private static final String[] PALETTE = {
        "#cfe2ff", "#d1f2d9", "#ffe3b3", "#f6d0e6", "#e2d9f3", "#d4f1f4", "#fff3bf", "#e9ecef"
    };

    private ModulesExport() {
    }

    /**
     * @param cp un seuil
     * @return sa forme courte pour un nom de fichier : 0.025 → « 0.025 »
     */
    public static String cpLabel(double cp) {
        return new BigDecimal(Double.toString(cp)).stripTrailingZeros().toPlainString();
    }

    /**
     * Écrit le CSV et le DOT d'une coupe.
     *
     * @param r   la coupe
     * @param d   le dendrogramme (pour les libellés)
     * @param g   le graphe de couplage (pour les arêtes du DOT)
     * @param dir dossier de destination (doit exister)
     * @return les deux fichiers écrits
     * @throws IOException en cas d'erreur d'écriture
     */
    public static List<Path> write(CutResult r, Dendrogram d, CouplingGraph g, Path dir)
            throws IOException {
        String base = "modules-cp" + cpLabel(r.cp());
        Path csv = dir.resolve(base + ".csv");
        Path dot = dir.resolve(base + ".dot");
        Files.writeString(csv, csv(r, d), StandardCharsets.UTF_8);
        Files.writeString(dot, dot(r, d, g), StandardCharsets.UTF_8);
        return List.of(csv, dot);
    }

    /**
     * @param r la coupe
     * @param d le dendrogramme
     * @return les modules au format CSV (séparateur {@code ;})
     */
    public static String csv(CutResult r, Dendrogram d) {
        StringBuilder sb = new StringBuilder();
        sb.append("cp;module;noeud;taille;couplageInterneMoyen;classes\n");
        int k = 1;
        for (CutResult.Module m : r.modules()) {
            List<String> names = new ArrayList<>();
            for (String q : d.labelsOf(m.cluster())) {
                names.add(Names.simple(q));
            }
            sb.append(String.format(Locale.ROOT, "%s;%d;G%d;%d;%s;%s%n",
                    cpLabel(r.cp()), k++, m.cluster().id(), m.size(),
                    Double.isNaN(m.internalCoupling()) ? "" : String.format(Locale.ROOT, "%.6f", m.internalCoupling()),
                    String.join(",", names)));
        }
        return sb.toString();
    }

    /**
     * @param r la coupe
     * @param d le dendrogramme
     * @param g le graphe de couplage
     * @return le graphe de couplage, un cadre par module, une couleur par paquetage
     */
    public static String dot(CutResult r, Dendrogram d, CouplingGraph g) {
        // on donne une couleur par paquetage, dans l'ordre alphabétique
        TreeSet<String> packages = new TreeSet<>();
        for (String q : d.labels()) {
            packages.add(Names.packageOf(q));
        }
        Map<String, String> color = new HashMap<>();
        int c = 0;
        for (String p : packages) {
            color.put(p, PALETTE[c++ % PALETTE.length]);
        }
        Map<String, Integer> moduleOf = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        sb.append("graph modules {\n");
        sb.append(String.format(Locale.ROOT,
                "  label=\"Modules à CP = %s : %d module(s), plafond M/2 = %d — %s\"; labelloc=t;%n",
                cpLabel(r.cp()), r.modules().size(), r.maxModules(),
                r.respectsMaxModules() ? "contrainte respectée" : "CONTRAINTE VIOLÉE"));
        sb.append("  rankdir=LR; nodesep=0.4; ranksep=1.0; fontname=\"Helvetica\";\n");
        sb.append("  node [shape=box, style=\"rounded,filled\", fontname=\"Helvetica\"];\n");
        sb.append("  edge [fontname=\"Helvetica\", fontsize=10];\n");

        // on ajoute la légende des paquetages
        sb.append("  subgraph cluster_legende {\n    label=\"paquetages\"; style=dotted;\n");
        int i = 0;
        for (String p : packages) {
            sb.append(String.format("    legende%d [label=\"%s\", fillcolor=\"%s\"];%n", i++, p, color.get(p)));
        }
        sb.append("  }\n");

        int k = 1;
        for (CutResult.Module m : r.modules()) {
            String title = m.size() == 1
                    ? String.format(Locale.ROOT, "M%d (1 classe)", k)
                    : String.format(Locale.ROOT, "M%d : %d classes, C̄ = %.4f", k, m.size(), m.internalCoupling());
            sb.append("  subgraph cluster_m").append(k).append(" {\n");
            sb.append("    label=\"").append(title).append("\"; style=rounded; color=\"#444444\";\n");
            for (String q : d.labelsOf(m.cluster())) {
                moduleOf.put(q, k);
                sb.append(String.format("    \"%s\" [label=\"%s\", fillcolor=\"%s\"];%n",
                        q, Names.simple(q), color.get(Names.packageOf(q))));
            }
            sb.append("  }\n");
            k++;
        }
        for (CouplingGraph.Edge e : g.edges()) {
            boolean internal = moduleOf.get(e.a()).equals(moduleOf.get(e.b()));
            sb.append(String.format("  \"%s\" -- \"%s\" [label=\"%d/%d\", penwidth=%d%s];%n",
                    e.a(), e.b(), e.r(), g.interArcs(), e.r(),
                    internal ? "" : ", style=dashed, color=\"#999999\", fontcolor=\"#999999\""));
        }
        sb.append("}\n");
        return sb.toString();
    }
}
