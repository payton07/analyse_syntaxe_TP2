package hai913i.tp2.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un nœud du dendrogramme : une feuille (une classe) ou une fusion de deux groupes.
 *
 * <p>Il porte ce que l'annexe § 4.1 demande :</p>
 * <ul>
 *   <li>son <b>identifiant</b> stable : 0..M−1 pour les feuilles (classes par ordre
 *       alphabétique), puis M, M+1… dans l'ordre de création — c'est lui que la
 *       règle de départage compare ;</li>
 *   <li>ses <b>deux fils</b>, pour reconstituer une branche (modules de B3) ;</li>
 *   <li>la <b>similarité de la fusion</b> — à ne pas confondre avec le couplage
 *       interne moyen du groupe, calculé à l'étape B3 ;</li>
 *   <li>ses <b>membres</b> triés, union disjointe de ceux des deux fils.</li>
 * </ul>
 *
 * <p>Objet immuable.</p>
 */
public final class Cluster {

    private final int id;
    private final Cluster left;
    private final Cluster right;
    private final double similarity;
    private final int step;
    private final List<Integer> members;

    private Cluster(int id, Cluster left, Cluster right, double similarity, int step,
                    List<Integer> members) {
        this.id = id;
        this.left = left;
        this.right = right;
        this.similarity = similarity;
        this.step = step;
        this.members = Collections.unmodifiableList(members);
    }

    /**
     * @param index indice de la classe, entre 0 et M − 1
     * @return la feuille correspondante
     */
    public static Cluster leaf(int index) {
        return new Cluster(index, null, null, Double.NaN, 0, List.of(index));
    }

    /**
     * Crée le nœud issu de la fusion de deux groupes.
     *
     * @param id         identifiant du nouveau groupe (M, M+1…)
     * @param left       le groupe d'identifiant le plus petit
     * @param right      l'autre groupe
     * @param similarity similarité entre les deux au moment de la fusion
     * @param step       numéro de la fusion, de 1 à M − 1
     * @return le nouveau nœud
     * @throws IllegalStateException si les deux groupes ont un membre commun
     */
    static Cluster merge(int id, Cluster left, Cluster right, double similarity, int step) {
        List<Integer> m = new ArrayList<>(left.members.size() + right.members.size());
        int i = 0;
        int j = 0;
        while (i < left.members.size() || j < right.members.size()) {   // on fusionne deux listes triées
            if (j == right.members.size()
                    || (i < left.members.size() && left.members.get(i) < right.members.get(j))) {
                m.add(left.members.get(i++));
            } else if (i == left.members.size() || right.members.get(j) < left.members.get(i)) {
                m.add(right.members.get(j++));
            } else {
                throw new IllegalStateException("groupes non disjoints : classe "
                        + left.members.get(i) + " dans " + left.id + " et " + right.id);
            }
        }
        return new Cluster(id, left, right, similarity, step, m);
    }

    /** @return l'identifiant stable du groupe */
    public int id() {
        return id;
    }

    /** @return vrai pour une feuille (une seule classe) */
    public boolean isLeaf() {
        return left == null;
    }

    /** @return le premier fils (identifiant le plus petit), ou {@code null} pour une feuille */
    public Cluster left() {
        return left;
    }

    /** @return le second fils, ou {@code null} pour une feuille */
    public Cluster right() {
        return right;
    }

    /** @return la similarité de la fusion qui a créé ce nœud ; NaN pour une feuille */
    public double similarity() {
        return similarity;
    }

    /** @return le numéro de la fusion qui a créé ce nœud (1..M−1) ; 0 pour une feuille */
    public int step() {
        return step;
    }

    /** @return les indices des classes du groupe, triés */
    public List<Integer> members() {
        return members;
    }

    /** @return le nombre de classes du groupe */
    public int size() {
        return members.size();
    }

    @Override
    public String toString() {
        return "G" + id + members;
    }
}
