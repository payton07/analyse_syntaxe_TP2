package hai913i.tp2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les faits d'entrée du TP 2.
 *
 * <p><b>Ce que cette classe représente.</b> L'ensemble des faits extraits d'un
 * projet Java : ses classes, ses méthodes, et les arcs de son graphe d'appel.
 * C'est exactement le modèle que le TP 1 produit, ramené à trois listes
 * neutres, sans aucune dépendance à l'outil qui les a extraites.</p>
 *
 * <p><b>Pourquoi elle existe.</b> Elle lit le jeu de données canonique et
 * le met à disposition sous forme d'objets. Elle ne calcule <em>aucun</em>
 * couplage, ne construit <em>aucun</em> graphe et ne regroupe rien : c'est de
 * la plomberie d'entrée, et l'étape A0 s'arrête là.</p>
 *
 * <p><b>Ce qu'elle ne fait délibérément pas.</b> Le couplage (étape A1), le
 * graphe pondéré (A2), le regroupement (B1), le dendrogramme (B2) et la coupe
 * (B3) sont l'objet du TP : rien de tout cela n'est fourni ici.</p>
 *
 * <p><b>Vous n'êtes pas obligés de l'utiliser.</b> Si vous branchez votre propre
 * modèle de faits du TP 1, écrivez un adaptateur qui produit les mêmes trois
 * vues — {@link #classNames()}, {@link #methodOwners()}, {@link #calls()} — et
 * le reste de votre programme n'aura pas à savoir d'où viennent les faits. Le
 * barème ne distingue pas les deux chemins.</p>
 *
 * <p><b>Format attendu</b> : trois fichiers CSV de même préfixe, séparateur
 * point-virgule, une ligne d'en-tête, encodage UTF-8. Aucun champ ne contient
 * de point-virgule. Le format complet est décrit dans
 * {@code resources/README.txt}.</p>
 *
 * <p><b>Invariants garantis après chargement</b> : les trois listes sont
 * immuables ; {@link #classNames()} est trié par nom qualifié, ce qui est
 * précisément l'ordre qui numérote les groupes dans la règle de départage du
 * sujet ; {@link #methodOwners()} contient <em>toutes</em> les méthodes du
 * projet, y compris celles qui n'apparaissent dans aucun appel.</p>
 */
public final class Facts {

    /**
     * Une classe du projet analysé.
     *
     * @param name        nom qualifié, p. ex. {@code library.model.Item}
     * @param simpleName  nom court, p. ex. {@code Item}
     * @param packageName paquetage, p. ex. {@code library.model}
     */
    public record ClassInfo(String name, String simpleName, String packageName) {
    }

    /**
     * Un arc du graphe d'appel : un couple (méthode appelante, méthode appelée),
     * compté <b>une seule fois</b>, avec le nombre de sites d'appel
     * correspondants.
     *
     * <p>La distinction est celle du § 5.2 du sujet : sur
     * le projet de validation, il y a 37 arcs inter-classes pour 38 sites
     * d'appel inter-classes. Le TP 2 compte des <b>arcs</b>, donc des lignes de
     * cette liste, et non la somme des {@code sites}.</p>
     *
     * @param caller      identifiant de la méthode appelante
     * @param callerClass nom qualifié de la classe qui la contient
     * @param callee      identifiant de la méthode appelée
     * @param calleeClass nom qualifié de la classe qui la contient
     * @param sites       nombre d'occurrences de cet appel dans le code source
     */
    public record CallArc(String caller, String callerClass,
                          String callee, String calleeClass, int sites) {
    }

    private final List<ClassInfo> classes;
    private final Map<String, String> methodOwners;
    private final List<CallArc> calls;

    private Facts(List<ClassInfo> classes, Map<String, String> methodOwners,
                  List<CallArc> calls) {
        this.classes = List.copyOf(classes);
        this.methodOwners = Collections.unmodifiableMap(methodOwners);
        this.calls = List.copyOf(calls);
    }

    /**
     * Charge un jeu de données depuis un dossier, sans connaître son préfixe.
     *
     * <p>Le préfixe est déduit du fichier {@code *-classes.csv} présent dans le
     * dossier. Votre outil fonctionne ainsi sur n'importe quel jeu de faits au
     * format canonique, et pas seulement sur celui du projet de validation.</p>
     *
     * @param dir dossier contenant les trois fichiers CSV
     * @return les faits chargés
     * @throws IOException si le dossier est absent, vide, ou contient plusieurs
     *                     jeux de données
     */
    public static Facts load(Path dir) throws IOException {
        return load(dir, discoverPrefix(dir));
    }

    /**
     * Déduit le préfixe commun des trois fichiers d'un dossier.
     *
     * @param dir dossier à examiner
     * @return le préfixe, par exemple {@code facts-validation}
     * @throws IOException si le dossier n'en contient pas exactement un
     */
    public static String discoverPrefix(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("dossier introuvable : " + dir.toAbsolutePath());
        }
        List<String> found = new ArrayList<>();
        try (var entries = Files.list(dir)) {
            for (Path p : entries.toList()) {
                String n = p.getFileName().toString();
                if (n.endsWith("-classes.csv")) {
                    found.add(n.substring(0, n.length() - "-classes.csv".length()));
                }
            }
        }
        Collections.sort(found);
        if (found.isEmpty()) {
            throw new IOException("aucun fichier *-classes.csv dans "
                    + dir.toAbsolutePath());
        }
        if (found.size() > 1) {
            throw new IOException("plusieurs jeux de données dans "
                    + dir.toAbsolutePath() + " : " + String.join(", ", found)
                    + " ; précisez le préfixe avec --prefix");
        }
        return found.get(0);
    }

    /**
     * Charge un jeu de données complet depuis un dossier, préfixe imposé.
     *
     * @param dir    dossier contenant les trois fichiers CSV
     * @param prefix préfixe commun, par exemple {@code facts-validation}
     * @return les faits chargés
     * @throws IOException si l'un des trois fichiers est absent ou mal formé
     */
    public static Facts load(Path dir, String prefix) throws IOException {
        return load(dir.resolve(prefix + "-classes.csv"),
                    dir.resolve(prefix + "-methods.csv"),
                    dir.resolve(prefix + "-calls.csv"));
    }

    /**
     * Charge les faits à partir des trois fichiers CSV d'un même jeu de données.
     *
     * <p>Les trois fichiers sont nécessaires, et le fichier des classes ne peut
     * pas se déduire des deux autres : deux classes du projet de validation
     * n'apparaissent dans aucun appel, et elles comptent pourtant dans M.</p>
     *
     * @param classesCsv fichier {@code ...-classes.csv} : une ligne par classe
     * @param methodsCsv fichier {@code ...-methods.csv} : une ligne par méthode
     * @param callsCsv   fichier {@code ...-calls.csv} : une ligne par arc
     * @return les faits chargés
     * @throws IOException si un fichier est absent, ou si une ligne n'a pas le
     *                     bon nombre de colonnes
     */
    public static Facts load(Path classesCsv, Path methodsCsv, Path callsCsv)
            throws IOException {
        List<ClassInfo> cls = new ArrayList<>();
        for (String[] f : readCsv(classesCsv, 3)) {
            cls.add(new ClassInfo(f[0], f[1], f[2]));
        }
        Map<String, String> owners = new LinkedHashMap<>();
        for (String[] f : readCsv(methodsCsv, 2)) {
            owners.put(f[0], f[1]);
        }
        List<CallArc> arcs = new ArrayList<>();
        for (String[] f : readCsv(callsCsv, 5)) {
            int sites;
            try {
                sites = Integer.parseInt(f[4].trim());
            } catch (NumberFormatException e) {
                throw new IOException("nombre de sites invalide : " + f[4]);
            }
            arcs.add(new CallArc(f[0], f[1], f[2], f[3], sites));
        }
        return new Facts(cls, owners, arcs);
    }

    /**
     * Lit un CSV à séparateur point-virgule et en renvoie les lignes de données.
     *
     * <p>Aucun champ du format canonique ne contient de point-virgule, de
     * guillemet ni de saut de ligne : un simple {@code split(";")} suffit, et
     * une bibliothèque CSV serait une dépendance pour rien.</p>
     *
     * @param path    fichier à lire, en UTF-8
     * @param columns nombre de colonnes attendu sur chaque ligne
     * @return les lignes de données, en-tête exclu, jamais nulles
     * @throws IOException fichier absent, ou ligne au mauvais nombre de colonnes
     */
    private static List<String[]> readCsv(Path path, int columns) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("fichier introuvable : " + path.toAbsolutePath());
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String[]> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {      // la ligne 0 est l'en-tête
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split(";", -1);
            if (fields.length != columns) {
                throw new IOException(path.getFileName() + " ligne " + (i + 1)
                        + " : " + columns + " colonnes attendues, "
                        + fields.length + " trouvées");
            }
            out.add(fields);
        }
        return out;
    }

    /**
     * Les classes du projet, dans l'ordre du fichier.
     *
     * @return liste immuable ; sa taille est le M du sujet
     */
    public List<ClassInfo> classes() {
        return classes;
    }

    /**
     * Les noms qualifiés des classes, triés.
     *
     * <p>Cet ordre est celui qui numérote les groupes de
     * 0 à M−1 dans la règle de départage des égalités (§ 5.4 du sujet). Deux
     * exécutions qui partent du même ordre donnent le même arbre.</p>
     *
     * @return nouvelle liste triée, modifiable sans effet sur cet objet
     */
    public List<String> classNames() {
        List<String> out = new ArrayList<>();
        for (ClassInfo c : classes) {
            out.add(c.name());
        }
        Collections.sort(out);
        return out;
    }

    /**
     * La classe propriétaire de chaque méthode comptée par le TP 1.
     *
     * <p>Y compris les méthodes qui n'apparaissent dans aucun appel.</p>
     *
     * @return table immuable, identifiant de méthode vers nom qualifié de classe
     */
    public Map<String, String> methodOwners() {
        return methodOwners;
    }

    /**
     * Tous les arcs du graphe d'appel, <b>arcs intra-classe compris</b>.
     *
     * <p>C'est à vous d'écarter les arcs dont les deux extrémités sont dans la
     * même classe (étape A1) : ils relèvent de la cohésion, pas du couplage, et
     * ils ne comptent ni au numérateur ni au dénominateur.</p>
     *
     * @return liste immuable ; sa taille est le compteur d'arcs de l'étape A0
     */
    public List<CallArc> calls() {
        return calls;
    }

    @Override
    public String toString() {
        return "Facts[" + classes.size() + " classes, " + methodOwners.size()
                + " méthodes, " + calls.size() + " arcs]";
    }
}
