package hai913i.tp2.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 * Le résultat du regroupement : l'arbre complet et la suite des fusions.
 *
 * <p>Les libellés ne servent qu'à la restitution : l'algorithme ne les a jamais
 * consultés (il ne voit que des indices).</p>
 *
 * @param labels les noms des classes, indice i → libellé de la feuille i
 * @param root   la racine, qui contient les M classes
 * @param merges les M − 1 fusions, dans l'ordre
 */
public record Dendrogram(List<String> labels, Cluster root, List<Merge> merges) {

    public Dendrogram {
        labels = List.copyOf(labels);
        merges = List.copyOf(merges);
    }

    /**
     * @param c un groupe
     * @return les libellés de ses membres, dans l'ordre de leurs indices
     */
    public List<String> labelsOf(Cluster c) {
        List<String> out = new ArrayList<>();
        for (int i : c.members()) {
            out.add(labels.get(i));
        }
        return out;
    }

    /** @return tous les nœuds de l'arbre, en profondeur, racine d'abord */
    public List<Cluster> allNodes() {
        List<Cluster> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(Cluster c, List<Cluster> out) {
        out.add(c);
        if (!c.isLeaf()) {
            collect(c.left(), out);
            collect(c.right(), out);
        }
    }
}
