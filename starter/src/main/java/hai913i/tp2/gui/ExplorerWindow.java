package hai913i.tp2.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import hai913i.tp2.clustering.Cluster;
import hai913i.tp2.clustering.Linkage;
import hai913i.tp2.cut.CutResult;
import hai913i.tp2.output.Names;

/**
 * La fenêtre de l'interface graphique (bonus C3).
 *
 * <p>Disposition : les réglages en haut (CP, lien, héritage, recherche),
 * l'arbre dépliable à gauche, le dendrogramme dessiné au centre, les modules
 * de la coupe à droite, et le détail du nœud sélectionné en bas.</p>
 *
 * <p>Les interactions et ce qu'elles montrent :</p>
 * <ul>
 *   <li><b>déplacer CP</b> : on voit à quel seuil la partition change, et à
 *       partir de quand la contrainte M/2 n'est plus respectée ;</li>
 *   <li><b>changer de lien ou ajouter l'héritage</b> : on compare les arbres
 *       et les modules des bonus C1 et C2 ;</li>
 *   <li><b>sélectionner un nœud</b> (dans l'arbre ou sur le dessin) : on lit
 *       côte à côte sa similarité de fusion et son couplage interne moyen,
 *       qu'il ne faut pas confondre ;</li>
 *   <li><b>chercher une classe</b> : on la surligne et on affiche son module
 *       et ses classes couplées.</li>
 * </ul>
 */
public final class ExplorerWindow {

    /** Le curseur va de 0 à 0,06 par pas de 0,0001 : il couvre toutes les coupes utiles. */
    private static final int SLIDER_MAX = 600;
    private static final double SLIDER_STEP = 1e-4;

    private final ExplorerModel model;
    private final JFrame frame;
    private final DendrogramPanel drawing;
    private final JTree tree = new JTree();
    private final DefaultTableModel modules = new DefaultTableModel(
            new Object[] {"Module", "Nœud", "Taille", "Coupl. int. moyen", "Classes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(modules);
    private final JLabel status = new JLabel();
    private final JTextArea details = new JTextArea(6, 80);
    private final JSlider slider = new JSlider(0, SLIDER_MAX);
    private final JTextField cpField = new JTextField(7);
    private final JTextField search = new JTextField(14);
    private boolean updating;          // vrai pendant une mise à jour faite par le programme

    private ExplorerWindow(ExplorerModel model, String title) {
        this.model = model;
        this.frame = new JFrame("HAI913I TP2 · " + title);
        this.drawing = new DendrogramPanel(model, this::select);
        build();
    }

    /**
     * Ouvre la fenêtre, sur le fil d'événements de Swing.
     *
     * @param model l'état à explorer
     * @param title le nom du jeu de données, pour la barre de titre
     */
    public static void open(ExplorerModel model, String title) {
        SwingUtilities.invokeLater(() -> new ExplorerWindow(model, title).frame.setVisible(true));
    }

    // -------------------------------------------------------------- construction

    private void build() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar(), BorderLayout.NORTH);

        tree.setRootVisible(true);
        tree.getSelectionModel().setSelectionMode(
                javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            if (updating || e.getPath() == null) {
                return;
            }
            Object node = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            if (node instanceof Item item) {
                select(item.cluster);
            }
        });

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(380);
        table.getColumnModel().getColumn(0).setCellRenderer(new ModuleColorRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!updating && !e.getValueIsAdjusting() && row >= 0) {
                select(model.cut().modules().get(row).cluster());
            }
        });
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        status.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(560, 400));
        right.add(status, BorderLayout.NORTH);
        right.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane centre = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(drawing), right);
        centre.setResizeWeight(1.0);              // le dessin prend la place libre
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), centre);
        main.setDividerLocation(290);

        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        details.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        details.setText("Cliquez sur un nœud de l'arbre, du dessin ou un module pour l'inspecter.");

        frame.add(main, BorderLayout.CENTER);
        frame.add(new JScrollPane(details), BorderLayout.SOUTH);

        refreshTree();
        refreshCut();
        frame.setPreferredSize(new Dimension(1600, 880));
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private JPanel toolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        slider.setPreferredSize(new Dimension(320, 40));
        slider.setValue((int) Math.round(model.cp() / SLIDER_STEP));
        slider.addChangeListener(e -> {
            if (!updating) {
                applyCp(slider.getValue() * SLIDER_STEP);
            }
        });
        cpField.setText(ExplorerModel.fmt(model.cp()));
        cpField.addActionListener(e -> {
            // dans l'interface, on accepte aussi la virgule : 0,025 n'y est pas ambigu
            String t = cpField.getText().strip().replace(',', '.');
            try {
                applyCp(Double.parseDouble(t));
            } catch (IllegalArgumentException ex) {     // NumberFormatException comprise
                cpField.setText(ExplorerModel.fmt(model.cp()));
                details.setText("Valeur de CP invalide : « " + t + " ». Donnez un nombre positif ou nul.");
            }
        });

        JComboBox<Linkage> linkage = new JComboBox<>(Linkage.values());
        linkage.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                          int index, boolean sel, boolean focus) {
                return super.getListCellRendererComponent(list, "lien " + ((Linkage) value).label(),
                        index, sel, focus);
            }
        });
        linkage.addActionListener(e -> {
            model.setLinkage((Linkage) linkage.getSelectedItem());
            refreshTree();
            refreshCut();
        });

        JCheckBox heritage = new JCheckBox("+ héritage (C2)");
        heritage.setEnabled(model.heritageAvailable());
        if (!model.heritageAvailable()) {
            heritage.setToolTipText("pas de fichier <préfixe>.json : héritage indisponible");
        }
        heritage.addActionListener(e -> {
            model.setHeritage(heritage.isSelected());
            refreshTree();
            refreshCut();
        });

        search.addActionListener(e -> runSearch());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                runSearch();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                runSearch();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                runSearch();
            }
        });

        bar.add(new JLabel("CP"));
        bar.add(slider);
        bar.add(cpField);
        bar.add(linkage);
        bar.add(heritage);
        bar.add(new JLabel("   Chercher une classe"));
        bar.add(search);
        return bar;
    }

    // ------------------------------------------------------------- interactions

    private void applyCp(double value) {
        model.setCp(value);                                  // lève une exception si invalide
        updating = true;
        slider.setValue((int) Math.min(SLIDER_MAX, Math.round(value / SLIDER_STEP)));
        cpField.setText(ExplorerModel.fmt(value));
        updating = false;
        refreshCut();
    }

    private void select(Cluster c) {
        drawing.setSelected(c);
        details.setText(model.describe(c));
        details.setCaretPosition(0);
        updating = true;
        selectInTree(c);
        int k = model.moduleOfNode(c);
        if (k >= 0 && model.cut().modules().get(k).cluster() == c) {
            table.setRowSelectionInterval(k, k);
        } else {
            table.clearSelection();
        }
        updating = false;
    }

    private void runSearch() {
        List<Integer> hits = model.search(search.getText());
        drawing.setHighlighted(hits);
        if (hits.size() == 1) {
            // une seule classe trouvée : on l'inspecte directement
            select(leafOf(model.dendrogram().root(), hits.get(0)));
        }
    }

    // -------------------------------------------------------------- rafraîchir

    /** On reconstruit l'arbre de gauche quand le dendrogramme change (lien, héritage). */
    private void refreshTree() {
        DefaultMutableTreeNode root = toNode(model.dendrogram().root());
        updating = true;
        tree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount() && i < 40; i++) {
            tree.expandRow(i);
        }
        updating = false;
        drawing.setSelected(null);
        drawing.revalidate();
    }

    /** On met à jour le tableau des modules et le statut M/2 après chaque coupe. */
    private void refreshCut() {
        CutResult cut = model.cut();
        updating = true;
        modules.setRowCount(0);
        int k = 1;
        for (CutResult.Module m : cut.modules()) {
            modules.addRow(new Object[] {
                "M" + k++,
                "G" + m.cluster().id(),
                m.size(),
                Double.isNaN(m.internalCoupling()) ? "—"
                        : String.format(Locale.ROOT, "%.4f", m.internalCoupling()),
                Names.braces(model.dendrogram().labelsOf(m.cluster())),
            });
        }
        updating = false;
        boolean ok = cut.respectsMaxModules();
        status.setText(String.format(Locale.ROOT,
                "CP = %s · lien %s%s · %d modules · plafond M/2 = %d · %s",
                ExplorerModel.fmt(cut.cp()), model.linkage().label(),
                model.heritage() ? " + héritage" : "", cut.modules().size(), cut.maxModules(),
                ok ? "respectée" : "VIOLÉE (coupe rejetée)"));
        status.setForeground(ok ? new Color(0x1b7f2a) : new Color(0xc62828));
        drawing.repaint();
        tree.repaint();
    }

    private void selectInTree(Cluster c) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        var e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            if (((Item) n.getUserObject()).cluster == c) {
                TreePath path = new TreePath(n.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
        }
    }

    private DefaultMutableTreeNode toNode(Cluster c) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(new Item(c));
        if (!c.isLeaf()) {
            n.add(toNode(c.left()));
            n.add(toNode(c.right()));
        }
        return n;
    }

    private static Cluster leafOf(Cluster c, int index) {
        if (c.isLeaf()) {
            return c.id() == index ? c : null;
        }
        Cluster l = leafOf(c.left(), index);
        return l != null ? l : leafOf(c.right(), index);
    }

    /** Ce que l'arbre de gauche affiche pour un nœud. */
    private final class Item {
        private final Cluster cluster;

        Item(Cluster cluster) {
            this.cluster = cluster;
        }

        @Override
        public String toString() {
            if (cluster.isLeaf()) {
                return Names.simple(model.dendrogram().labels().get(cluster.id()));
            }
            return String.format(Locale.ROOT, "G%d · fusion %d · S %.4f · %d classes",
                    cluster.id(), cluster.step(), cluster.similarity(), cluster.size());
        }
    }

    /** La première colonne du tableau prend la couleur du module, comme sur le dessin. */
    private static final class ModuleColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            c.setForeground(DendrogramPanel.PALETTE[row % DendrogramPanel.PALETTE.length]);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            return c;
        }
    }
}
