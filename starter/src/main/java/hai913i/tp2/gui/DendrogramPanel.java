package hai913i.tp2.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Dendrogram;
import hai913i.tp2.output.Names;

/**
 * Le dendrogramme dessiné, couché : les classes à gauche, une colonne par
 * fusion, la racine à droite.
 *
 * <p>On place les fusions à intervalle régulier, dans leur ordre, plutôt qu'à
 * une abscisse proportionnelle à la similarité : avec des valeurs entre 0 et
 * 0,108, une échelle proportionnelle tasserait la moitié de l'arbre contre la
 * racine (même choix que pour l'export DOT). La similarité s'affiche au survol
 * et dans le panneau de détail.</p>
 *
 * <p>Les branches prennent la couleur de leur module pour la coupe courante ;
 * ce qui est au-dessus des modules reste gris. Chaque racine de module porte
 * une pastille. Un clic sur un nœud le sélectionne.</p>
 */
final class DendrogramPanel extends JPanel implements Scrollable {

    /** Couleurs des modules, assez distinctes entre elles ; on boucle au-delà. */
    static final Color[] PALETTE = {
        new Color(0x1f77b4), new Color(0xd62728), new Color(0x2ca02c), new Color(0x9467bd),
        new Color(0xff7f0e), new Color(0x8c564b), new Color(0xe377c2), new Color(0x17becf),
        new Color(0xbcbd22), new Color(0x7f7f7f),
    };
    private static final Color GREY = new Color(0xb0b0b0);
    private static final int ROW = 30;
    private static final int TOP = 30;
    private static final int RIGHT = 40;

    private final ExplorerModel model;
    private final Consumer<Cluster> onSelect;
    private final Map<Cluster, int[]> positions = new HashMap<>();
    private Cluster selected;
    private Set<Integer> highlighted = new HashSet<>();

    DendrogramPanel(ExplorerModel model, Consumer<Cluster> onSelect) {
        this.model = model;
        this.onSelect = onSelect;
        setBackground(Color.WHITE);
        setToolTipText("");                       // active les infobulles
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Cluster c = nodeAt(e.getX(), e.getY());
                if (c != null) {
                    onSelect.accept(c);
                }
            }
        });
    }

    void setSelected(Cluster c) {
        selected = c;
        repaint();
    }

    void setHighlighted(List<Integer> classes) {
        highlighted = new HashSet<>(classes);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int m = model.dendrogram().labels().size();
        return new Dimension(560, TOP * 2 + m * ROW);
    }

    // on suit la largeur de la zone visible : le dessin s'étire au lieu de défiler à l'horizontale

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
        return ROW;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
        return ROW * 5;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Cluster c = nodeAt(e.getX(), e.getY());
        if (c == null) {
            return null;
        }
        if (c.isLeaf()) {
            return Names.simple(model.dendrogram().labels().get(c.id()));
        }
        return String.format(Locale.ROOT, "G%d · fusion %d · S = %.4f · couplage interne moyen = %.4f",
                c.id(), c.step(), c.similarity(), model.internalCoupling(c));
    }

    private Cluster nodeAt(int x, int y) {
        for (Map.Entry<Cluster, int[]> e : positions.entrySet()) {
            int[] p = e.getValue();
            if (Math.abs(p[0] - x) <= 7 && Math.abs(p[1] - y) <= 7) {
                return e.getKey();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ dessin

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Dendrogram d = model.dendrogram();
        g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        FontMetrics fm = g.getFontMetrics();

        // on mesure la plus longue étiquette pour savoir où commence l'arbre
        int labelWidth = 0;
        for (String l : d.labels()) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(Names.simple(l)));
        }
        int x0 = 20 + labelWidth + 12;
        int steps = Math.max(1, d.merges().size());
        double dx = Math.max(24.0, (getWidth() - x0 - RIGHT) / (double) steps);

        positions.clear();
        List<Cluster> leaves = new ArrayList<>();
        collectLeaves(d.root(), leaves);
        for (int i = 0; i < leaves.size(); i++) {
            positions.put(leaves.get(i), new int[] {x0, TOP + i * ROW + ROW / 2});
        }
        place(d.root(), x0, dx);

        // axe du haut : une graduation par fusion
        g.setColor(Color.GRAY);
        g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        for (int s = 1; s <= steps; s++) {
            int x = (int) Math.round(x0 + s * dx);
            g.drawString(Integer.toString(s), x - 3, TOP - 12);
        }
        g.setFont(getFont().deriveFont(Font.PLAIN, 13f));

        drawBranches(g, d.root());

        // étiquettes des classes, colorées par module ; les résultats de recherche surlignés
        for (Cluster leaf : leaves) {
            int[] p = positions.get(leaf);
            String label = Names.simple(d.labels().get(leaf.id()));
            int w = fm.stringWidth(label);
            int lx = x0 - 12 - w;
            if (highlighted.contains(leaf.id())) {
                g.setColor(new Color(0xfff176));
                g.fillRoundRect(lx - 4, p[1] - fm.getAscent() + 1, w + 8, fm.getHeight(), 6, 6);
            }
            g.setColor(colorOf(leaf));
            g.drawString(label, lx, p[1] + fm.getAscent() / 2 - 2);
        }

        // pastille sur la racine de chaque module, cercle autour de la sélection
        for (Map.Entry<Cluster, int[]> e : positions.entrySet()) {
            Cluster c = e.getKey();
            int[] p = e.getValue();
            int k = model.moduleOfNode(c);
            boolean moduleRoot = k >= 0 && model.cut().modules().get(k).cluster() == c;
            if (moduleRoot) {
                g.setColor(PALETTE[k % PALETTE.length]);
                g.fillOval(p[0] - 6, p[1] - 6, 12, 12);
                g.setColor(Color.BLACK);
                g.setFont(getFont().deriveFont(Font.BOLD, 10f));
                g.drawString("M" + (k + 1), p[0] + 7, p[1] - 6);
                g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
            } else if (!c.isLeaf()) {
                g.setColor(colorOf(c));
                g.fillOval(p[0] - 3, p[1] - 3, 6, 6);
            }
            if (c == selected) {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(p[0] - 9, p[1] - 9, 18, 18);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }

    private void place(Cluster c, int x0, double dx) {
        if (c.isLeaf()) {
            return;
        }
        place(c.left(), x0, dx);
        place(c.right(), x0, dx);
        int y = (positions.get(c.left())[1] + positions.get(c.right())[1]) / 2;
        positions.put(c, new int[] {(int) Math.round(x0 + c.step() * dx), y});
    }

    private void drawBranches(Graphics2D g, Cluster c) {
        if (c.isLeaf()) {
            return;
        }
        int[] p = positions.get(c);
        g.setStroke(new BasicStroke(2f));
        for (Cluster child : List.of(c.left(), c.right())) {
            int[] q = positions.get(child);
            g.setColor(colorOf(child));
            g.drawLine(q[0], q[1], p[0], q[1]);             // segment horizontal du fils
        }
        int[] a = positions.get(c.left());
        int[] b = positions.get(c.right());
        g.setColor(colorOf(c));
        g.drawLine(p[0], a[1], p[0], b[1]);                 // barre verticale de la fusion
        g.setStroke(new BasicStroke(1f));
        drawBranches(g, c.left());
        drawBranches(g, c.right());
    }

    private Color colorOf(Cluster c) {
        int k = model.moduleOfNode(c);
        return k < 0 ? GREY : PALETTE[k % PALETTE.length];
    }

    private static void collectLeaves(Cluster c, List<Cluster> out) {
        if (c.isLeaf()) {
            out.add(c);
        } else {
            collectLeaves(c.left(), out);
            collectLeaves(c.right(), out);
        }
    }
}
