package hai913i.tp2.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Regroupement hiérarchique ascendant, lien moyen (étapes B1 et B2).
 *
 * <p><b>Algorithme.</b> Au départ, chaque classe forme un groupe. Tant qu'il
 * reste au moins deux groupes actifs, on fusionne les deux plus similaires. On
 * s'arrête après exactement M − 1 fusions : il ne reste que la racine.</p>
 *
 * <p><b>Lien moyen</b> (§ 5.3) :
 * S(G1, G2) = Σ<sub>a∈G1</sub> Σ<sub>b∈G2</sub> C(a, b) / (|G1| · |G2|).
 * Le dénominateur compte <em>tous</em> les couples, y compris ceux de couplage
 * nul. La similarité est toujours <b>recalculée à partir des couplages entre
 * classes</b>, jamais déduite des anciennes similarités de groupes (annexe § 2).</p>
 *
 * <p><b>Départage des égalités</b> (§ 5.4). Les groupes actifs sont parcourus par
 * identifiant croissant, donc les couples (i, j), i &lt; j, dans l'ordre
 * lexicographique. Un couple ne remplace le meilleur courant que s'il est
 * <em>strictement</em> meilleur, au-delà d'une tolérance {@link #EPS} : à égalité,
 * le premier rencontré — le plus petit — est conservé.</p>
 *
 * <p>Complexité de l'ordre de M³ lectures de similarité : négligeable pour M = 15,
 * et c'est la version la plus simple à vérifier (annexe § 8).</p>
 */
public final class AverageLinkClustering {

    /** Tolérance de comparaison des similarités (§ 5.4 : jamais d'égalité stricte). */
    public static final double EPS = 1e-9;

    private AverageLinkClustering() {
    }

    /**
     * Construit le dendrogramme.
     *
     * @param labels libellés des M classes (pour la restitution seulement) ;
     *               l'indice i de la liste est l'identifiant de la feuille i
     * @param sim    similarité entre classes, indexée de 0 à M − 1
     * @return l'arbre et les M − 1 fusions
     * @throws IllegalArgumentException s'il n'y a aucune classe
     */
    public static Dendrogram run(List<String> labels, Similarity sim) {
        int m = labels.size();
        if (m == 0) {
            throw new IllegalArgumentException("aucune classe à regrouper");
        }
        // groupes actifs, triés par identifiant : c'est ce tri qui rend le parcours déterministe
        TreeMap<Integer, Cluster> active = new TreeMap<>();
        for (int i = 0; i < m; i++) {
            active.put(i, Cluster.leaf(i));
        }
        List<Merge> merges = new ArrayList<>();
        int nextId = m;
        for (int step = 1; step < m; step++) {
            List<Cluster> groups = new ArrayList<>(active.values());
            Cluster bestA = null;
            Cluster bestB = null;
            double best = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < groups.size(); i++) {
                for (int j = i + 1; j < groups.size(); j++) {
                    double s = averageLink(groups.get(i), groups.get(j), sim);
                    if (bestA == null || s > best + EPS) {   // strictement meilleur seulement
                        best = s;
                        bestA = groups.get(i);
                        bestB = groups.get(j);
                    }
                }
            }
            Cluster merged = Cluster.merge(nextId++, bestA, bestB, best, step);
            active.remove(bestA.id());
            active.remove(bestB.id());
            active.put(merged.id(), merged);
            merges.add(new Merge(step, bestA, bestB, best, merged));
        }
        return new Dendrogram(labels, active.firstEntry().getValue(), merges);
    }

    /**
     * Lien moyen entre deux groupes disjoints, recalculé à partir des couplages
     * entre classes.
     *
     * @param g1  un groupe
     * @param g2  un autre groupe
     * @param sim similarité entre classes
     * @return S(g1, g2) = somme des C(a, b) / (|g1| · |g2|)
     */
    public static double averageLink(Cluster g1, Cluster g2, Similarity sim) {
        double sum = 0.0;
        for (int a : g1.members()) {
            for (int b : g2.members()) {
                sum += sim.of(a, b);
            }
        }
        return sum / ((double) g1.size() * g2.size());   // TOUS les couples, nuls compris
    }
}
