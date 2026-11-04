package hai913i.tp2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Point d'entrée du squelette : vérifie l'environnement et charge les faits.
 *
 * <p><b>Ce qu'il fait.</b> Il lit les arguments de la ligne de commande, charge
 * un jeu de faits au format canonique, et affiche les trois compteurs du
 * <b>point de contrôle A0</b> : nombre de classes, de méthodes, d'arcs du
 * graphe d'appel.</p>
 *
 * <p><b>Ce qu'il ne fait pas.</b> Tout le reste. Le couplage (A1), le graphe
 * pondéré (A2), le regroupement (B1), le dendrogramme (B2) et la coupe (B3)
 * sont l'objet du TP : leurs emplacements sont marqués {@code TODO} ci-dessous,
 * et rien n'en est fourni.</p>
 *
 * <pre>
 *   mvn clean package
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS
 *   java -jar target/hai913i-tp2.jar CHEMIN_DU_DOSSIER_DES_FAITS --cp 0.025
 * </pre>
 *
 * <p>Le dossier passé en argument est celui qui contient les trois fichiers
 * {@code ...-classes.csv}, {@code ...-methods.csv} et {@code ...-calls.csv}.
 * Le préfixe en est déduit : votre outil n'a donc aucune raison de connaître le
 * nom du projet de validation, et il fonctionnera tel quel sur un autre jeu de
 * faits..</p>
 *
 * <p><b>Codes de sortie</b> : 0 si les faits sont chargés, 1 si les arguments
 * sont invalides, 2 si les fichiers sont illisibles. Gardez cette convention
 * dans votre propre outil : elle fait partie du critère de reproductibilité.</p>
 */
public final class Main {

    /**
     * Sortie standard forcée en UTF-8.
     *
     * <p>Sans cela, une console Windows configurée sur une autre page de codes
     * afficherait les accents de travers. Le jeu de données, les sources et
     * cette sortie sont tous en UTF-8.</p>
     */
    private static final PrintStream OUT =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** Sortie d'erreur, forcée en UTF-8 pour la même raison. */
    private static final PrintStream ERR =
            new PrintStream(System.err, true, StandardCharsets.UTF_8);

    /** Seuil de coupe par défaut, repris du point de contrôle B3 du sujet. */
    private static final double CP_DEFAUT = 0.025;

    private Main() {
    }

    /**
     * Charge les faits et affiche les compteurs de l'étape A0.
     *
     * @param args {@code <dossier-des-faits> [--cp <valeur>] [--prefix <préfixe>]}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage("chemin du dossier des faits manquant");
            return;
        }
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        String prefix = null;
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i]) && i + 1 < args.length) {
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    usage("valeur de --cp invalide : " + args[i]);
                    return;
                }
            } else if ("--prefix".equals(args[i]) && i + 1 < args.length) {
                prefix = args[++i];
            } else {
                usage("argument inconnu : " + args[i]);
                return;
            }
        }
        if (cp < 0.0) {
            usage("--cp doit être positif ou nul");
            return;
        }

        Facts facts;
        try {
            if (prefix == null) {
                prefix = Facts.discoverPrefix(dir);
            }
            facts = Facts.load(dir, prefix);
        } catch (IOException e) {
            ERR.println("Lecture des faits impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        OUT.println("Dossier des faits   : " + dir.toAbsolutePath());
        OUT.println("Jeu de données      : " + prefix);
        OUT.printf(Locale.ROOT, "Classes             : %d%n",
                facts.classes().size());
        OUT.printf(Locale.ROOT, "Méthodes            : %d%n",
                facts.methodOwners().size());
        OUT.printf(Locale.ROOT, "Arcs du graphe      : %d%n",
                facts.calls().size());
        OUT.printf(Locale.ROOT, "Seuil CP retenu     : %.4f%n", cp);

        // TODO étape A1 : écarter les arcs intra-classe, agréger les arcs
        //                 restants par couple de classes distinctes, puis
        //                 calculer C(A, B) = r(A, B) / |E_inter|.
        // TODO étape A2 : construire et exporter le graphe de couplage pondéré.
        // TODO étape B1 : regroupement hiérarchique ascendant, lien moyen,
        //                 égalités départagées comme le sujet l'impose.
        // TODO étape B2 : représenter et exporter le dendrogramme.
        // TODO étape B3 : couper l'arbre sous les trois contraintes, puis
        //                 vérifier la contrainte M/2 et signaler sa violation.
        OUT.println();
        OUT.println("Faits chargés. Les étapes A1 à B3 restent à écrire.");
    }

    /**
     * Affiche un message d'erreur, le mode d'emploi, et sort avec le code 1.
     *
     * @param message cause de l'échec, en une ligne
     */
    private static void usage(String message) {
        ERR.println("Erreur : " + message);
        ERR.println();
        ERR.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits>"
                + " [--cp <valeur>] [--prefix <préfixe>]");
        ERR.println();
        ERR.println("  <dossier-des-faits>  dossier contenant les trois fichiers");
        ERR.println("                       <préfixe>-classes.csv,");
        ERR.println("                       <préfixe>-methods.csv et");
        ERR.println("                       <préfixe>-calls.csv");
        ERR.println("  --cp <valeur>        seuil de couplage interne moyen"
                + " (défaut : " + CP_DEFAUT + ")");
        ERR.println("  --prefix <préfixe>   force le préfixe ; par défaut il est");
        ERR.println("                       déduit du dossier");
        System.exit(1);
    }
}
