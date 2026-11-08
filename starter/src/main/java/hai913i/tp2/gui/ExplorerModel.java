package hai913i.tp2.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.clustering.HierarchicalClustering;
import hai913i.tp2.clustering.Linkage;
import hai913i.tp2.clustering.Similarity;
import hai913i.tp2.coupling.Coupling;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.cut.TreeCut;
import hai913i.tp2.input.FactSource;
import hai913i.tp2.input.Inheritance;
import hai913i.tp2.output.Names;

/**
 * L'état de l'interface graphique (bonus C3), sans aucune dépendance à Swing.
 *
 * <p>On sépare cet état de la fenêtre pour deux raisons : on peut le tester en
 * JUnit sans écran, et l'interface n'a pas de calcul à elle. Elle appelle
 * exactement les mêmes classes que la ligne de commande ({@link Coupling},
 * {@link HierarchicalClustering}, {@link TreeCut}) : ce qu'on voit à l'écran
 * est donc ce que calcule l'outil, au chiffre près.</p>
 *
 * <p>Trois réglages : le seuil CP, le critère de liaison (bonus C1) et
 * l'ajout de l'héritage (bonus C2). Changer CP ne refait que la coupe ;
 * changer le lien ou l'héritage reconstruit l'arbre, puis la coupe.</p>
 */
public final class ExplorerModel {

    private final FactSource facts;
    private final List<Inheritance.Link> links;     // null si le JSON est absent

    private Linkage linkage = Linkage.MOYEN;
    private boolean heritage;
    private double cp;

    private Coupling coupling;
    private Similarity sim;
    private Dendrogram dendrogram;
    private CutResult cut;

    /**
     * @param facts les faits chargés
     * @param links les liens d'héritage, ou {@code null} si on n'a pas pu les
     *              lire : la case « héritage » est alors désactivée
     * @param cp    le seuil de départ
     */
    public ExplorerModel(FactSource facts, List<Inheritance.Link> links, double cp) {
        this.facts = facts;
        this.links = links == null ? null : List.copyOf(links);
        this.cp = cp;
        rebuildTree();
    }

    // ----------------------------------------------------------------- réglages

    /** @return vrai si les liens d'héritage sont disponibles (bonus C2) */
    public boolean heritageAvailable() {
        return links != null;
    }

    /** @param l le nouveau critère de liaison ; on reconstruit l'arbre s'il change */
    public void setLinkage(Linkage l) {
        if (l != linkage) {
            linkage = l;
            rebuildTree();
        }
    }

    /**
     * @param on vrai pour compter aussi l'héritage
     * @throws IllegalStateException si l'héritage n'est pas disponible
     */
    public void setHeritage(boolean on) {
        if (on && !heritageAvailable()) {
            throw new IllegalStateException("héritage indisponible : pas de fichier JSON");
        }
        if (on != heritage) {
            heritage = on;
            rebuildTree();
        }
    }

    /**
     * @param value le nouveau seuil ; on ne refait que la coupe
     * @throws IllegalArgumentException si le seuil est négatif ou n'est pas un nombre
     */
    public void setCp(double value) {
        cut = TreeCut.cut(dendrogram, sim, value);   // vérifie aussi la valeur
        cp = value;
    }

    private void rebuildTree() {
        coupling = Coupling.from(facts, heritage ? links : List.of());
        sim = Similarity.fromMatrix(coupling.matrix());
        dendrogram = HierarchicalClustering.run(coupling.classes(), sim, linkage);
        cut = TreeCut.cut(dendrogram, sim, cp);
    }

    public Linkage linkage() {
        return linkage;
    }

    public boolean heritage() {
        return heritage;
    }

    public double cp() {
        return cp;
    }

    public Coupling coupling() {
        return coupling;
    }

    public Dendrogram dendrogram() {
        return dendrogram;
    }

    public CutResult cut() {
        return cut;
    }

    // ------------------------------------------------------------- requêtes

    /**
     * @param classIndex indice d'une classe (feuille)
     * @return le rang de son module dans la coupe courante (0 pour M1), ou −1
     */
    public int moduleOfClass(int classIndex) {
        List<CutResult.Module> mods = cut.modules();
        for (int k = 0; k < mods.size(); k++) {
            if (mods.get(k).cluster().members().contains(classIndex)) {
                return k;
            }
        }
        return -1;
    }

    /**
     * Un nœud appartient à un module s'il est dans sa branche : tous ses
     * membres sont dans le module. Un nœud au-dessus des modules (refusé par la
     * coupe) n'appartient à aucun.
     *
     * @param c un nœud de l'arbre courant
     * @return le rang du module qui le contient, ou −1
     */
    public int moduleOfNode(Cluster c) {
        int k = moduleOfClass(c.members().get(0));
        if (k >= 0 && cut.modules().get(k).cluster().members().containsAll(c.members())) {
            return k;
        }
        return -1;
    }

    /**
     * @param c un nœud
     * @return son couplage interne moyen C̄, NaN pour une feuille
     */
    public double internalCoupling(Cluster c) {
        return TreeCut.internalCoupling(c, sim);
    }

    /**
     * @param text un morceau de nom de classe, sans tenir compte de la casse
     * @return les indices des classes dont le nom court le contient ; vide si
     *         le texte est vide
     */
    public List<Integer> search(String text) {
        List<Integer> out = new ArrayList<>();
        String t = text.strip().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return out;
        }
        List<String> labels = dendrogram.labels();
        for (int i = 0; i < labels.size(); i++) {
            if (Names.simple(labels.get(i)).toLowerCase(Locale.ROOT).contains(t)) {
                out.add(i);
            }
        }
        return out;
    }

    /**
     * Le texte affiché quand on sélectionne un nœud : ce qu'il faut pour
     * comprendre pourquoi la coupe l'a gardé ou non.
     *
     * @param c un nœud de l'arbre courant
     * @return une description de quelques lignes
     */
    public String describe(Cluster c) {
        StringBuilder sb = new StringBuilder();
        int k = moduleOfNode(c);
        String module = k >= 0 ? "dans le module M" + (k + 1) : "au-dessus des modules (refusé par la coupe)";
        if (c.isLeaf()) {
            String name = dendrogram.labels().get(c.id());
            sb.append(Names.simple(name)).append("  (").append(name).append(", feuille #")
              .append(c.id()).append(")\n");
            sb.append("Module : ").append(module).append('\n');
            sb.append("Classes couplées : ");
            List<String> neighbours = new ArrayList<>();
            for (int j = 0; j < dendrogram.labels().size(); j++) {
                String other = dendrogram.labels().get(j);
                int r = coupling.r(name, other);
                if (r > 0) {
                    neighbours.add(Names.simple(other) + " " + r + "/" + coupling.interArcs());
                }
            }
            sb.append(neighbours.isEmpty() ? "aucune (classe isolée)" : String.join(", ", neighbours));
            return sb.toString();
        }
        sb.append(String.format(Locale.ROOT, "G%d — fusion n° %d, %d classes%n", c.id(), c.step(), c.size()));
        sb.append(String.format(Locale.ROOT,
                "Similarité de la fusion (lien %s) : %.4f   — proximité ENTRE ses deux fils%n",
                linkage.label(), c.similarity()));
        double avg = internalCoupling(c);
        sb.append(String.format(Locale.ROOT,
                "Couplage interne moyen  : %.4f   — cohésion À L'INTÉRIEUR ; %s CP = %s%n",
                avg, avg > cp + TreeCut.EPS ? ">" : "≤", fmt(cp)));
        sb.append("Position : ").append(module).append('\n');
        sb.append("Classes : ").append(Names.braces(dendrogram.labelsOf(c)));
        return sb.toString();
    }

    /** @return CP sans zéros inutiles : 0.025 et non 0.025000 */
    static String fmt(double v) {
        return new java.math.BigDecimal(Double.toString(v)).stripTrailingZeros().toPlainString();
    }
}
