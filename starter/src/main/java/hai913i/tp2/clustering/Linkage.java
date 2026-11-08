package hai913i.tp2.clustering;

/**
 * Le critère de liaison : comment on mesure la similarité entre deux groupes à
 * partir des similarités entre classes.
 *
 * <p>Le sujet impose le lien moyen (§ 5.3). On ajoute le lien simple pour le
 * bonus C1, afin de comparer les deux arbres sur le même graphe de couplage.
 * Seul ce calcul change : la boucle de fusion et la règle de départage restent
 * les mêmes dans {@link HierarchicalClustering}.</p>
 */
public enum Linkage {

    /**
     * Lien moyen : la moyenne de C(a, b) sur <em>tous</em> les couples, nuls
     * compris. C'est le critère imposé.
     */
    MOYEN("moyen") {
        @Override
        public double between(Cluster g1, Cluster g2, Similarity sim) {
            double sum = 0.0;
            for (int a : g1.members()) {
                for (int b : g2.members()) {
                    sum += sim.of(a, b);
                }
            }
            return sum / ((double) g1.size() * g2.size());   // on divise par TOUS les couples, nuls compris
        }
    },

    /**
     * Lien simple : la similarité du <em>meilleur</em> couple. Un seul arc fort
     * suffit à rapprocher deux groupes, d'où l'effet de chaîne qu'on observe.
     */
    SIMPLE("simple") {
        @Override
        public double between(Cluster g1, Cluster g2, Similarity sim) {
            double best = 0.0;                                // le couplage n'est jamais négatif
            for (int a : g1.members()) {
                for (int b : g2.members()) {
                    best = Math.max(best, sim.of(a, b));
                }
            }
            return best;
        }
    };

    private final String label;

    Linkage(String label) {
        this.label = label;
    }

    /** @return le nom employé sur la ligne de commande et dans les sorties */
    public String label() {
        return label;
    }

    /**
     * Similarité entre deux groupes disjoints, recalculée à partir des
     * similarités entre classes.
     *
     * @param g1  un groupe
     * @param g2  un autre groupe
     * @param sim similarité entre classes
     * @return S(g1, g2) selon ce critère
     */
    public abstract double between(Cluster g1, Cluster g2, Similarity sim);

    /**
     * @param text {@code moyen} ou {@code simple}, sans tenir compte de la casse
     * @return le critère correspondant, ou {@code null} s'il est inconnu
     */
    public static Linkage parse(String text) {
        for (Linkage l : values()) {
            if (l.label.equalsIgnoreCase(text.trim())) {
                return l;
            }
        }
        return null;
    }
}
