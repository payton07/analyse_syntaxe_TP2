package hai913i.tp2.clustering;

/**
 * Une fusion du regroupement ascendant.
 *
 * @param step       numéro de la fusion, de 1 à M − 1
 * @param a          le groupe d'identifiant le plus petit
 * @param b          l'autre groupe
 * @param similarity S(a, b) au moment de la fusion (lien moyen)
 * @param result     le groupe créé, d'identifiant M + step − 1
 */
public record Merge(int step, Cluster a, Cluster b, double similarity, Cluster result) {
}
