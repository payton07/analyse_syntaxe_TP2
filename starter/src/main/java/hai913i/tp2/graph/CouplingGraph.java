package hai913i.tp2.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import hai913i.tp2.coupling.ClassPair;
import hai913i.tp2.coupling.Coupling;

/**
 * Le graphe de couplage pondéré (étape A2).
 *
 * <p>Un nœud par classe — <b>y compris</b> les classes isolées, qui n'ont aucune
 * arête mais comptent dans M —, une arête non orientée par couple de couplage
 * non nul, de poids C(A, B) = r / |E_inter|.</p>
 *
 * <p>Objet immuable. Les nœuds sont triés par nom qualifié et les arêtes par
 * couple, ce qui rend les exports identiques d'une exécution à l'autre.</p>
 */
public final class CouplingGraph {

    /**
     * Une arête non orientée.
     *
     * @param a      la plus petite des deux classes (ordre alphabétique)
     * @param b      la plus grande
     * @param r      nombre d'arcs inter-classes entre a et b
     * @param weight poids C(a, b) = r / |E_inter|
     */
    public record Edge(String a, String b, int r, double weight) {
    }

    private final List<String> nodes;
    private final List<Edge> edges;
    private final int interArcs;

    /**
     * Constructeur commun au calcul et à la relecture d'un export.
     *
     * @param nodes     les classes, triées
     * @param edges     les arêtes, triées par couple
     * @param interArcs |E_inter|, conservé pour réécrire les poids sous forme r/|E|
     */
    CouplingGraph(List<String> nodes, List<Edge> edges, int interArcs) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.interArcs = interArcs;
    }

    /**
     * Construit le graphe à partir du couplage.
     *
     * @param c le couplage de l'étape A1
     * @return le graphe pondéré
     */
    public static CouplingGraph from(Coupling c) {
        List<Edge> edges = new ArrayList<>();
        for (Map.Entry<ClassPair, Integer> e : c.nonZeroPairs().entrySet()) {
            ClassPair p = e.getKey();
            edges.add(new Edge(p.first(), p.second(), e.getValue(),
                    c.c(p.first(), p.second())));
        }
        return new CouplingGraph(c.classes(), edges, c.interArcs());
    }

    /** @return les nœuds (toutes les classes), triés */
    public List<String> nodes() {
        return nodes;
    }

    /** @return les arêtes, triées par couple */
    public List<Edge> edges() {
        return edges;
    }

    /** @return |E_inter| */
    public int interArcs() {
        return interArcs;
    }

    /**
     * @param x une classe
     * @param y une classe
     * @return le poids de l'arête {x, y}, ou 0 si elle n'existe pas
     */
    public double weight(String x, String y) {
        for (Edge e : edges) {
            if ((e.a().equals(x) && e.b().equals(y)) || (e.a().equals(y) && e.b().equals(x))) {
                return e.weight();
            }
        }
        return 0.0;
    }

    /** @return les nœuds sans aucune arête, triés */
    public List<String> isolated() {
        Set<String> touched = new HashSet<>();
        for (Edge e : edges) {
            touched.add(e.a());
            touched.add(e.b());
        }
        List<String> out = new ArrayList<>();
        for (String n : nodes) {
            if (!touched.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * @param k nombre d'arêtes voulues
     * @return les k arêtes les plus lourdes ; à poids égal, par ordre des couples
     */
    public List<Edge> topEdges(int k) {
        List<Edge> sorted = new ArrayList<>(edges);
        sorted.sort(Comparator.comparingInt(Edge::r).reversed()
                .thenComparing(Edge::a).thenComparing(Edge::b));
        return sorted.subList(0, Math.min(k, sorted.size()));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CouplingGraph g
                && interArcs == g.interArcs && nodes.equals(g.nodes) && edges.equals(g.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges, interArcs);
    }

    @Override
    public String toString() {
        return "CouplingGraph[" + nodes.size() + " nœuds, " + edges.size() + " arêtes]";
    }
}
