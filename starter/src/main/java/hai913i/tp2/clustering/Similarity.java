package hai913i.tp2.clustering;

/**
 * Ce que le regroupement sait des classes : rien d'autre qu'un nombre pour
 * chaque couple d'indices.
 *
 * <p><b>Pourquoi cette abstraction</b> (annexe § 1). Le regroupement ne doit rien
 * savoir des faits : ni noms, ni méthodes, ni arcs. Il ne manipule que les
 * indices 0..M−1 des classes et cette fonction. On peut ainsi le tester sur une
 * matrice écrite à la main, et brancher une autre mesure (bonus C2) sans toucher
 * à la boucle de fusion.</p>
 */
@FunctionalInterface
public interface Similarity {

    /**
     * @param i indice d'une classe, entre 0 et M − 1
     * @param j indice d'une autre classe
     * @return la similarité entre les deux classes (le couplage C pour ce TP)
     */
    double of(int i, int j);

    /**
     * Enveloppe une matrice carrée M × M.
     *
     * @param matrix matrice symétrique ; elle est copiée
     * @return la fonction de similarité correspondante
     * @throws IllegalArgumentException si la matrice n'est pas carrée
     */
    static Similarity fromMatrix(double[][] matrix) {
        int m = matrix.length;
        double[][] copy = new double[m][];
        for (int i = 0; i < m; i++) {
            if (matrix[i].length != m) {
                throw new IllegalArgumentException("matrice non carrée à la ligne " + i);
            }
            copy[i] = matrix[i].clone();
        }
        return (i, j) -> copy[i][j];
    }
}
