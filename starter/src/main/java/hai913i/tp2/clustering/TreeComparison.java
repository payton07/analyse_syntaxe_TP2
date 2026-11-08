package hai913i.tp2.clustering;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Comparaison de deux dendrogrammes construits sur les mêmes classes (bonus C1).
 *
 * <p>On compare les fusions étape par étape, par leur <b>contenu</b> : deux
 * fusions sont identiques si elles réunissent les mêmes deux ensembles de
 * classes. On ne compare pas les identifiants, parce qu'après une première
 * divergence ils ne désignent plus les mêmes groupes dans les deux arbres.</p>
 */
public final class TreeComparison {

    private TreeComparison() {
    }

    /**
     * @param d1 un dendrogramme
     * @param d2 un autre dendrogramme, sur les mêmes classes
     * @return le numéro de la première fusion qui diffère, ou 0 si les deux
     *         suites de fusions sont identiques
     * @throws IllegalArgumentException si les deux arbres ne portent pas sur
     *                                  les mêmes classes
     */
    public static int firstDivergence(Dendrogram d1, Dendrogram d2) {
        if (!d1.labels().equals(d2.labels())) {
            throw new IllegalArgumentException("les deux arbres ne portent pas sur les mêmes classes");
        }
        List<Merge> m1 = d1.merges();
        List<Merge> m2 = d2.merges();
        for (int k = 0; k < m1.size(); k++) {
            if (!sameMerge(m1.get(k), m2.get(k))) {
                return k + 1;
            }
        }
        return 0;
    }

    /**
     * @param a une fusion
     * @param b une autre fusion
     * @return vrai si elles réunissent les mêmes deux ensembles de classes,
     *         quel que soit leur ordre
     */
    public static boolean sameMerge(Merge a, Merge b) {
        Set<List<Integer>> x = pair(a);
        Set<List<Integer>> y = pair(b);
        return Objects.equals(x, y);
    }

    private static Set<List<Integer>> pair(Merge m) {
        Set<List<Integer>> s = new TreeSet<>((p, q) -> p.toString().compareTo(q.toString()));
        s.add(m.a().members());
        s.add(m.b().members());
        return s;
    }
}
