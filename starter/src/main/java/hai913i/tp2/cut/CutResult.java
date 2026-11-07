package hai913i.tp2.cut;

import java.util.List;

import hai913i.tp2.clustering.Cluster;

/**
 * Le résultat d'une coupe (étape B3) : toujours une partition des M classes,
 * mais pas forcément une partition <em>acceptable</em>.
 *
 * <p>La coupe n'est jamais « réparée » : si elle produit plus de M/2 modules, elle
 * est rapportée telle quelle avec {@link #respectsMaxModules()} à faux (annexe § 5).</p>
 *
 * @param cp         le seuil utilisé
 * @param modules    les modules, dans l'ordre du parcours (fils gauche d'abord)
 * @param classCount M, le nombre de classes
 */
public record CutResult(double cp, List<Module> modules, int classCount) {

    /**
     * Un module : une branche du dendrogramme retenue par la coupe.
     *
     * @param cluster          le nœud de l'arbre (une feuille pour un module d'une classe)
     * @param internalCoupling couplage interne moyen C̄(G) ; NaN pour une feuille
     *                         (aucun couple, la contrainte ne s'applique pas)
     */
    public record Module(Cluster cluster, double internalCoupling) {

        /** @return le nombre de classes du module */
        public int size() {
            return cluster.size();
        }
    }

    public CutResult {
        modules = List.copyOf(modules);
    }

    /** @return M/2 en division entière : le nombre maximal de modules autorisé */
    public int maxModules() {
        return classCount / 2;
    }

    /** @return vrai si la coupe contient au plus M/2 modules */
    public boolean respectsMaxModules() {
        return modules.size() <= maxModules();
    }

    /** @return la taille du plus gros module */
    public int largestModuleSize() {
        return modules.stream().mapToInt(Module::size).max().orElse(0);
    }
}
