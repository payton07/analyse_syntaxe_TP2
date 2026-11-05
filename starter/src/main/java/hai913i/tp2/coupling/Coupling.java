package hai913i.tp2.coupling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import hai913i.tp2.input.FactSource;
import hai913i.tp2.input.FactSource.CallArc;

/**
 * Le couplage entre classes (étape A1).
 *
 * <p><b>Définition retenue (CM 03, § 5.2 du sujet).</b> Pour deux classes
 * distinctes A et B :</p>
 * <pre>
 *   C(A, B) = r(A, B) / |E_inter|
 * </pre>
 * <ul>
 *   <li>r(A, B) : nombre d'<b>arcs</b> du graphe d'appel reliant une méthode de A
 *       à une méthode de B, dans un sens ou dans l'autre ;</li>
 *   <li>E_inter : les arcs dont les deux extrémités sont dans deux classes
 *       distinctes ; les arcs internes à une classe sont écartés (cohésion) ;</li>
 *   <li>C(A, A) = 0 et C(A, B) = C(B, A).</li>
 * </ul>
 *
 * <p><b>Ce qu'on compte.</b> Un arc est une ligne du fichier des appels, comptée
 * une fois quel que soit son nombre de sites d'appel.</p>
 *
 * <p><b>Stockage.</b> Table creuse et triée des seuls couples non nuls ; un couple
 * absent vaut 0. Le graphe de validation n'a que 19 couples non nuls sur 105.</p>
 *
 * <p>Objet immuable : une fois construit, rien ne le modifie.</p>
 */
public final class Coupling {

    private final List<String> classes;
    private final SortedMap<ClassPair, Integer> arcsByPair;
    private final int interArcs;
    private final int internalArcs;

    private Coupling(List<String> classes, SortedMap<ClassPair, Integer> arcsByPair,
                     int interArcs, int internalArcs) {
        this.classes = List.copyOf(classes);
        this.arcsByPair = Collections.unmodifiableSortedMap(arcsByPair);
        this.interArcs = interArcs;
        this.internalArcs = internalArcs;
    }

    /**
     * Calcule le couplage à partir des faits.
     *
     * @param facts les faits, derrière l'interface d'entrée
     * @return le couplage entre toutes les classes
     * @throws IllegalArgumentException si un arc cite une classe absente de la
     *                                  liste des classes (faits incohérents)
     */
    public static Coupling from(FactSource facts) {
        List<String> classes = facts.classNames();
        Set<String> known = new HashSet<>(classes);
        SortedMap<ClassPair, Integer> arcs = new TreeMap<>();
        int inter = 0;
        int internal = 0;
        for (CallArc arc : facts.calls()) {
            String a = arc.callerClass();
            String b = arc.calleeClass();
            if (!known.contains(a) || !known.contains(b)) {
                throw new IllegalArgumentException("arc vers une classe inconnue : "
                        + arc.caller() + " -> " + arc.callee());
            }
            if (a.equals(b)) {
                internal++;                  // arc interne : relève de la cohésion
                continue;
            }
            inter++;                         // un arc = +1, quels que soient ses sites
            arcs.merge(ClassPair.of(a, b), 1, Integer::sum);
        }
        return new Coupling(classes, arcs, inter, internal);
    }

    /** @return les classes, triées ; leur nombre est M */
    public List<String> classes() {
        return classes;
    }

    /** @return |E_inter|, le dénominateur du couplage */
    public int interArcs() {
        return interArcs;
    }

    /** @return le nombre d'arcs internes à une classe, écartés du calcul */
    public int internalArcs() {
        return internalArcs;
    }

    /**
     * @param a une classe
     * @param b une classe
     * @return r(a, b), le nombre d'arcs entre a et b (0 si a = b ou sans arc)
     */
    public int r(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        return arcsByPair.getOrDefault(ClassPair.of(a, b), 0);
    }

    /**
     * @param a une classe
     * @param b une classe
     * @return C(a, b) = r(a, b) / |E_inter| ; 0 si a = b, ou s'il n'existe aucun
     *         arc inter-classes (pas de division par zéro)
     */
    public double c(String a, String b) {
        if (interArcs == 0) {
            return 0.0;
        }
        return (double) r(a, b) / interArcs;
    }

    /** @return les couples de couplage non nul et leur r, triés par couple */
    public SortedMap<ClassPair, Integer> nonZeroPairs() {
        return arcsByPair;
    }

    /**
     * Les couples non nuls par couplage décroissant ; à couplage égal, par ordre
     * des noms (affichage déterministe).
     *
     * @return nouvelle liste
     */
    public List<Map.Entry<ClassPair, Integer>> byDecreasingCoupling() {
        List<Map.Entry<ClassPair, Integer>> out = new ArrayList<>(arcsByPair.entrySet());
        out.sort(Comparator.comparing((Map.Entry<ClassPair, Integer> e) -> e.getValue())
                .reversed()
                .thenComparing(Map.Entry::getKey));
        return out;
    }

    /**
     * Somme de C(A, B) sur tous les couples de classes distinctes. Vaut 1 dès
     * qu'il existe au moins un arc inter-classes : c'est le contrôle le plus
     * rentable du TP (double comptage, mauvais dénominateur, arcs oubliés).
     *
     * @return la somme des couplages
     */
    public double total() {
        double sum = 0.0;
        for (ClassPair p : arcsByPair.keySet()) {
            sum += c(p.first(), p.second());
        }
        return sum;
    }

    /**
     * La matrice M × M des couplages, dans l'ordre de {@link #classes()} : la
     * case [i][j] vaut C(classe i, classe j). Symétrique, diagonale nulle.
     *
     * <p>C'est la seule chose que le regroupement reçoit : des indices 0..M−1 et
     * des nombres, sans aucun nom de classe ni aucun arc.</p>
     *
     * @return une nouvelle matrice
     */
    public double[][] matrix() {
        int m = classes.size();
        double[][] x = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                double v = c(classes.get(i), classes.get(j));
                x[i][j] = v;
                x[j][i] = v;
            }
        }
        return x;
    }

    /** @return M (M − 1) / 2, le nombre de couples de classes distinctes */
    public int possiblePairs() {
        int m = classes.size();
        return m * (m - 1) / 2;
    }
}
