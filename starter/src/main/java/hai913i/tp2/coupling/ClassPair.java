package hai913i.tp2.coupling;

import java.util.Objects;

/**
 * Un couple <b>non orienté</b> de deux classes distinctes.
 *
 * <p><b>Pourquoi ce type.</b> Au niveau des classes, le sens de l'appel est perdu
 * (§ 5.2) : l'arc A → B et l'arc B → A doivent tomber sur la <em>même</em> clé.
 * La fabrique {@link #of(String, String)} range donc toujours les deux noms par
 * ordre alphabétique ({@code first < second}). Sans cette normalisation, chaque
 * relation serait comptée sur deux clés et la somme des couplages vaudrait 2.</p>
 *
 * <p>Le couple est {@link Comparable} (premier nom, puis second) : parcourir une
 * collection triée de couples donne toujours le même ordre, d'une exécution à
 * l'autre.</p>
 *
 * @param first  le plus petit des deux noms qualifiés
 * @param second le plus grand des deux noms qualifiés
 */
public record ClassPair(String first, String second) implements Comparable<ClassPair> {

    /**
     * Constructeur canonique : vérifie que le couple est déjà normalisé.
     * Passez par {@link #of(String, String)} pour construire un couple.
     */
    public ClassPair {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.compareTo(second) >= 0) {
            throw new IllegalArgumentException(
                    "couple non normalisé ou réflexif : (" + first + ", " + second + ")");
        }
    }

    /**
     * Construit le couple non orienté {x, y}, quel que soit l'ordre des arguments.
     *
     * @param x une classe
     * @param y une autre classe
     * @return le couple normalisé
     * @throws IllegalArgumentException si {@code x} et {@code y} sont la même classe
     *                                  (un couple réflexif n'a pas de couplage)
     */
    public static ClassPair of(String x, String y) {
        return x.compareTo(y) < 0 ? new ClassPair(x, y) : new ClassPair(y, x);
    }

    @Override
    public int compareTo(ClassPair o) {
        int c = first.compareTo(o.first);
        return c != 0 ? c : second.compareTo(o.second);
    }

    @Override
    public String toString() {
        return first + " – " + second;
    }
}
