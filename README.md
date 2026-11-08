# HAI913I — TP 2 : Compréhension et architectures

Outil en ligne de commande qui part des faits extraits d'un projet Java (classes, méthodes, graphe d'appel) et en tire des **modules candidats** : couplage entre classes, graphe pondéré, clustering hiérarchique ascendant au lien moyen, dendrogramme, coupe sous contraintes. Trois bonus : lien simple comparé au lien moyen (C1), héritage ajouté à la relation (C2), interface graphique (C3).

**Vidéo de démonstration** : [`video/demonstration.mp4`](https://github.com/payton07/analyse_syntaxe_TP2/blob/main/video/demonstration.mp4) (dans ce dépôt).

## 1. Projet

### Structure

```
.
├── resources/                 jeu de données canonique (faits du projet de validation)
└── starter/                   projet Maven
    ├── pom.xml
    └── src/
        ├── main/java/hai913i/tp2/
        │   ├── Main.java      enchaîne les étapes, affiche, codes de sortie
        │   ├── input/         A0 — FactSource (interface d'entrée), Facts (lecteur CSV) ; C2 — Inheritance (JSON)
        │   ├── coupling/      A1 — ClassPair, Coupling : C(A,B) = r(A,B) / |E_inter|
        │   ├── graph/         A2 — CouplingGraph, GraphIO (CSV rechargeable, DOT)
        │   ├── clustering/    B1 — Similarity, Cluster, Merge, Dendrogram, HierarchicalClustering ; C1 — Linkage, TreeComparison
        │   ├── cut/           B3 — TreeCut, CutResult (seuil CP, contrainte M/2)
        │   ├── output/        B2/B3 — Names, DendrogramExport, ModulesExport
        │   └── gui/           C3 — ExplorerModel (état, testé), ExplorerWindow, DendrogramPanel (Swing)
        └── test/              76 tests JUnit 5 + micro jeu et copie du jeu de validation
```

### Architecture

Chaîne `input → coupling → graph → clustering → cut → output`, orchestrée par `Main`. Le paquetage `clustering` ne dépend d'aucun autre : il ne reçoit qu'une matrice de similarité indexée par entiers. L'interface graphique (`gui`) appelle exactement les mêmes classes que la ligne de commande.

### Chemin d'entrée retenu

**Chemin 2** du sujet : le jeu de données canonique (`resources/`), lu par `Facts`. Le reste du programme ne connaît que l'interface `input.FactSource`.

### Entrées attendues

Un dossier contenant trois fichiers CSV de même préfixe, séparés par `;`, en UTF-8 :
`<préfixe>-classes.csv`, `<préfixe>-methods.csv`, `<préfixe>-calls.csv` (format décrit dans `resources/README.txt`). Le préfixe est déduit du dossier, ou donné avec `--prefix`. La lecture vérifie l'en-tête exact de chaque fichier, retire un éventuel BOM et refuse les guillemets : un fichier qui ne suit pas ce format est signalé (code 2), jamais lu de travers.

Pour le bonus C2 seulement (`--heritage`) : `<préfixe>.json`, dont on lit le bloc `extra.inheritance`.

### Sorties produites

Sur la console : les compteurs et points de contrôle de chaque étape. Dans le dossier de sortie (`sorties/` par défaut) :

| Fichier | Contenu |
|---|---|
| `graphe-couplage.csv` | graphe pondéré, rechargeable (`kind;a;b;r;interArcs;weight`) |
| `graphe-couplage.dot` | graphe pondéré, classes regroupées par paquetage |
| `dendrogramme.txt` | arbre indenté : étape, similarité, membres de chaque nœud |
| `fusions.csv` | une ligne par fusion |
| `dendrogramme.dot` | image du dendrogramme |
| `modules-cp<CP>.csv` | modules d'une coupe : taille, couplage interne moyen, classes |
| `modules-cp<CP>.dot` | graphe : un cadre par module, une couleur par paquetage |

Avec `--lien simple` ou `--heritage`, les fichiers du dendrogramme et des modules portent le suffixe `-simple`, `-heritage` ou `-simple-heritage` (et le graphe `-heritage`) : les sorties de référence ne sont jamais écrasées.

### Dépendances

Gson 2.11.0, uniquement pour lire le JSON du bonus C2 ; le plugin `maven-shade-plugin` 3.6.0 l'embarque dans le jar, qui reste autonome. JUnit Jupiter 5.14.4 pour les tests. Les deux sont téléchargés par Maven. L'interface graphique utilise Swing, inclus dans le JDK. Graphviz est facultatif, seulement pour transformer les `.dot` en images.

### Limites connues

- Une seule relation comptée : l'appel de méthode (héritage, interfaces, `new`, exceptions ignorés, comme imposé).
- Une coupe qui viole `M/2` est signalée, jamais corrigée.
- Pas d'adaptateur pour le modèle de faits du TP 1 (chemin 1) ; le bonus C4 (second projet) n'est pas traité.
- C2 : un lien d'héritage compte comme un arc d'appel ; les autres relations (types d'attributs, `new`…) ne sont pas dans les faits fournis.

## 2. Environnement

| Outil | Version utilisée |
|---|---|
| JDK | Temurin 21.0.5 (le projet compile en Java 17, `release 17`) |
| Maven | 3.9.9 |
| JUnit Jupiter | 5.14.4 |
| Gson | 2.11.0 (bonus C2) |
| maven-shade-plugin | 3.6.0 |
| Graphviz | 16.1.0 (facultatif) |
| Système | macOS 27.2 |
| IDE | IntelliJ IDEA 2025.3.2 (non requis : tout se fait en ligne de commande) |

## 3. Construction

Depuis une copie fraîche, sans IDE :

```sh
git clone https://github.com/payton07/analyse_syntaxe_TP2.git
cd analyse_syntaxe_TP2/starter
mvn clean package
```

`mvn clean package` compile, exécute les 76 tests et produit `target/hai913i-tp2.jar` (Gson compris). Pour les tests seuls : `mvn clean test`.

## 4. Exécution

Depuis `starter/` :

```sh
java -jar target/hai913i-tp2.jar <dossier-des-faits> [--cp v1,v2,…] [--prefix p] [--out dossier]
                                  [--lien moyen|simple] [--heritage] [--gui]
```

| Argument | Rôle | Défaut |
|---|---|---|
| `<dossier-des-faits>` | dossier contenant les trois CSV | obligatoire |
| `--cp` | un ou plusieurs seuils de couplage interne moyen, séparés par des virgules, positifs ou nuls | `0.025` |
| `--prefix` | préfixe des fichiers (`facts-validation`…) | déduit du dossier |
| `--out` | dossier des exports, créé s'il n'existe pas | `sorties` |
| `--lien` | critère de liaison ; `simple` affiche aussi la comparaison avec le lien moyen (C1) | `moyen` |
| `--heritage` | ajoute l'héritage lu dans `<préfixe>.json` et compare les modules avec et sans (C2) | non |
| `--gui` | ouvre l'interface graphique au lieu de la sortie console (C3) ; CP de départ = premier seuil de `--cp` | non |

Exemples :

```sh
# valeur retenue
java -jar target/hai913i-tp2.jar ../resources --cp 0.025

# plusieurs seuils, dont deux qui violent M/2, avec un tableau récapitulatif
java -jar target/hai913i-tp2.jar ../resources --cp 0.015,0.025,0.035,0.045

# bonus C1 : lien simple et première étape de divergence avec le lien moyen
java -jar target/hai913i-tp2.jar ../resources --lien simple

# bonus C2 : héritage ajouté, modules comparés avec et sans
java -jar target/hai913i-tp2.jar ../resources --heritage --cp 0.02,0.025

# bonus C3 : interface graphique (curseur CP, lien, héritage, recherche, inspection)
java -jar target/hai913i-tp2.jar ../resources --gui

# images (Graphviz)
dot -Tpng sorties/graphe-couplage.dot -o sorties/graphe-couplage.png
dot -Tpng sorties/dendrogramme.dot    -o sorties/dendrogramme.png
dot -Tpng sorties/modules-cp0.025.dot -o sorties/modules-cp0.025.png
```

Codes de sortie : `0` exécution complète (y compris une coupe qui viole `M/2`), `1` arguments invalides, `2` faits illisibles ou incohérents, exports impossibles à écrire.

## 5. Validation

Jeu de données : `resources/` (projet de validation du TP 1). Points de contrôle attendus et obtenus :

| Étape | Attendu |
|---|---|
| A0 | 15 classes, 59 méthodes, 44 arcs |
| A1 | 7 arcs internes écartés, `|E_inter| = 37`, 19 couples non nuls sur 105, Item–LoanService = 4/37 = 0,1081, somme = 1,0000 |
| A2 | 15 nœuds, 19 arêtes, Dvd et LoanException isolées, « Rechargement du CSV : identique » |
| B1 | 14 fusions ; les trois premières à 0,1081, 0,0811, 0,0608 ; similarités qui ne remontent jamais |
| B2 | 15 feuilles, 14 nœuds internes, deux dernières fusions à 0 |
| B3 | CP 0,015 → 4 modules ; 0,025 → 7 (respectée tout juste) ; 0,035 → 9 et 0,045 → 10 (violée) |
| C1 | `--lien simple` : première divergence à l'étape 2 (Loan rejoint {Item, LoanService} à 0,1081) |
| C2 | `--heritage` : 4 liens comptés, 3 écartés, `|E_inter| = 41` ; CP 0,025 → 8 modules (violée), 0,02 → 7 |

Contrôles automatiques : `mvn clean test` (76 tests). Déterminisme : deux exécutions donnent une sortie identique octet par octet :

```sh
java -jar target/hai913i-tp2.jar ../resources --cp 0.015,0.025,0.045 > run1.txt
java -jar target/hai913i-tp2.jar ../resources --cp 0.015,0.025,0.045 > run2.txt
cmp run1.txt run2.txt && echo identiques
```

## 6. Dépannage

| Symptôme | Cause et remède |
|---|---|
| `error: release version 17 not supported` | JDK trop ancien : installer un JDK 17 ou plus, vérifier `java -version` et `mvn -v`. |
| `mvn: command not found` | Maven absent du `PATH` : l'installer (3.9 ou plus). |
| `Unable to access jarfile target/hai913i-tp2.jar` | Le jar n'est pas construit, ou la commande n'est pas lancée depuis `starter/` : `mvn clean package`. |
| `Lecture des faits impossible : aucun fichier *-classes.csv` (code 2) | Mauvais dossier : passer le dossier qui contient les trois CSV (`../resources` depuis `starter/`). |
| `valeur de --cp invalide` (code 1) | Valeur non numérique, négative ou `NaN` : donner un nombre positif ou nul. |
| Avec `--cp 0,025`, deux coupes s'affichent (CP = 0 et CP = 25) | La virgule sépare plusieurs seuils : le séparateur décimal est le point, `--cp 0.025`. |
| `format non reconnu, en-tête … attendu` ou `guillemet trouvé` (code 2) | Le CSV ne suit pas le format canonique (colonnes renommées ou déplacées, autre séparateur, champs entre guillemets) : le régénérer au format de `resources/README.txt`. |
| `Lecture de l'héritage impossible : fichier introuvable … .json` (code 2) | `--heritage` demande `<préfixe>.json` à côté des CSV : l'ajouter, ou retirer l'option. |
| `Interface graphique impossible : aucun écran disponible` (code 2) | `--gui` lancé sans affichage (SSH, serveur) : lancer depuis une session graphique, ou utiliser la sortie console. |
| `Écriture des exports impossible` (code 2) | `--out` désigne un fichier, ou un dossier sans droit d'écriture. |
| Accents illisibles dans la console Windows | Passer la console en UTF-8 : `chcp 65001`. |
| `dot: command not found` | Graphviz absent : l'installer, ou ouvrir les `.dot` dans un visualiseur en ligne. |
| Contrainte `M/2` « VIOLÉE » | Ce n'est pas une erreur : le seuil CP est trop élevé pour ce projet ; baisser CP. |
