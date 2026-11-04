package hai913i.tp2.input;

import java.util.List;
import java.util.Map;

/**
 * L'unique interface d'entrée du TP 2 (étape A0).
 *
 * <p><b>Pourquoi elle existe.</b> Le sujet ouvre deux chemins pour obtenir les
 * faits : le modèle du TP 1 (chemin 1) ou le jeu de données canonique
 * (chemin 2). Tout le reste du programme ne connaît que cette interface ; changer
 * de chemin revient à fournir une autre implémentation, sans rien toucher au
 * couplage, au regroupement ni à la coupe.</p>
 *
 * <p>Implémentation fournie : {@link Facts}, qui lit les CSV canoniques. Pour le
 * chemin 1, il suffit d'écrire un adaptateur qui expose le modèle du TP 1 sous
 * ces trois vues.</p>
 */
public interface FactSource {

    /**
     * Un arc du graphe d'appel : un couple (méthode appelante, méthode appelée),
     * compté <b>une seule fois</b>, avec le nombre de sites d'appel
     * correspondants.
     *
     * <p>Le TP 2 compte des <b>arcs</b> (les éléments de {@link #calls()}),
     * pas la somme des {@code sites} (§ 5.2 du sujet).</p>
     *
     * @param caller      identifiant de la méthode appelante
     * @param callerClass nom qualifié de la classe qui la contient
     * @param callee      identifiant de la méthode appelée
     * @param calleeClass nom qualifié de la classe qui la contient
     * @param sites       nombre d'occurrences de cet appel dans le code source
     */
    record CallArc(String caller, String callerClass,
                   String callee, String calleeClass, int sites) {
    }

    /**
     * Les noms qualifiés de <b>toutes</b> les classes du projet, triés par ordre
     * alphabétique. Leur nombre est le M du sujet, et cet ordre numérote les
     * classes de 0 à M − 1 pour la règle de départage (§ 5.4).
     *
     * @return liste triée, y compris les classes qui n'apparaissent dans aucun appel
     */
    List<String> classNames();

    /**
     * La classe propriétaire de chaque méthode.
     *
     * @return table identifiant de méthode → nom qualifié de classe
     */
    Map<String, String> methodOwners();

    /**
     * Tous les arcs du graphe d'appel, <b>arcs intra-classe compris</b>.
     *
     * @return liste des arcs ; l'étape A1 se charge d'écarter les arcs internes
     */
    List<CallArc> calls();
}
