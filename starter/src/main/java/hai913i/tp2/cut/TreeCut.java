package hai913i.tp2.cut;

import java.util.ArrayList;
import java.util.List;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.Similarity;

/**
 * Coupe du dendrogramme sous les trois contraintes du CM 03 (étape B3).
 *
 * <p><b>Règle.</b> Parcours en profondeur depuis la racine ; à chaque nœud, une
 * seule question :</p>
 * <ul>
 *   <li>feuille → module d'une seule classe (aucun couple, la contrainte de
 *       moyenne ne s'applique pas) ;</li>
 *   <li>C̄(G) &gt; CP, <b>strictement</b> → le nœud devient un module, on ne
 *       descend pas plus bas ;</li>
 *   <li>sinon → on descend dans les <b>deux</b> fils.</li>
 * </ul>
 * <p>Chaque module est donc par construction une branche de l'arbre. La
 * contrainte M/2 est vérifiée <em>après</em> la coupe, et seulement signalée.</p>
 *
 * <p><b>Couplage interne moyen</b> C̄(G) : moyenne de C sur <em>tous</em> les
 * |G|(|G|−1)/2 couples de classes du groupe, nuls compris. Ce n'est <em>pas</em>
 * la similarité de la fusion qui a créé le nœud (celle-ci mesure la proximité
 * <em>entre</em> ses deux fils).</p>
 */
public final class TreeCut {

    /**
     * Tolérance : C̄ doit dépasser CP de plus de EPS. Une égalité au dernier bit
     * près compte donc comme « non supérieur », conformément au « strictement ».
     */
    public static final double EPS = 1e-9;

    private TreeCut() {
    }

    /**
     * Coupe l'arbre au seuil CP.
     *
     * @param d   le dendrogramme
     * @param sim la similarité entre classes (le couplage C)
     * @param cp  le seuil de couplage interne moyen, positif ou nul
     * @return la partition obtenue, acceptable ou non
     * @throws IllegalArgumentException si {@code cp} est négatif ou n'est pas un nombre
     */
    public static CutResult cut(Dendrogram d, Similarity sim, double cp) {
        if (Double.isNaN(cp) || Double.isInfinite(cp) || cp < 0.0) {
            throw new IllegalArgumentException("CP doit être un nombre positif ou nul : " + cp);
        }
        List<CutResult.Module> modules = new ArrayList<>();
        visit(d.root(), sim, cp, modules);
        return new CutResult(cp, modules, d.labels().size());
    }

    private static void visit(Cluster c, Similarity sim, double cp, List<CutResult.Module> out) {
        if (c.isLeaf()) {
            out.add(new CutResult.Module(c, Double.NaN));
            return;
        }
        double avg = internalCoupling(c, sim);
        if (avg > cp + EPS) {
            out.add(new CutResult.Module(c, avg));
        } else {
            visit(c.left(), sim, cp, out);
            visit(c.right(), sim, cp, out);
        }
    }

    /**
     * Couplage interne moyen d'un groupe d'au moins deux classes.
     *
     * @param g   le groupe
     * @param sim la similarité entre classes
     * @return Σ C(a, b) sur les couples {a, b} ⊆ G, divisé par |G|(|G|−1)/2 ;
     *         NaN pour une feuille (aucun couple)
     */
    public static double internalCoupling(Cluster g, Similarity sim) {
        List<Integer> m = g.members();
        int n = m.size();
        if (n < 2) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += sim.of(m.get(i), m.get(j));
            }
        }
        return sum / (n * (n - 1) / 2.0);     // on divise par TOUS les couples, nuls compris
    }
}
