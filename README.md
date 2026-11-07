# HAI913I — TP 2 : Compréhension et architectures

Outil en ligne de commande qui part des faits extraits d'un projet Java (classes, méthodes, graphe d'appel) et en tire des **modules candidats** : couplage entre classes, graphe pondéré, clustering hiérarchique ascendant au lien moyen, dendrogramme, coupe sous contraintes.

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
        │   ├── input/         A0 — FactSource (interface d'entrée), Facts (lecteur CSV)
        │   ├── coupling/      A1 — ClassPair, Coupling : C(A,B) = r(A,B) / |E_inter|
        │   ├── graph/         A2 — CouplingGraph, GraphIO (CSV rechargeable, DOT)
        │   ├── clustering/    B1 — Similarity, Cluster, Merge, Dendrogram, AverageLinkClustering
        │   ├── cut/           B3 — TreeCut, CutResult (seuil CP, contrainte M/2)
        │   └── output/        B2/B3 — Names, DendrogramExport, ModulesExport
        └── test/              56 tests JUnit 5 + micro jeu et copie du jeu de validation
```

### Architecture

Chaîne `input → coupling → graph → clustering → cut → output`, orchestrée par `Main`. Le paquetage `clustering` ne dépend d'aucun autre : il ne reçoit qu'une matrice de similarité indexée par entiers.

### Chemin d'entrée retenu

**Chemin 2** du sujet : le jeu de données canonique (`resources/`), lu par `Facts`. Le reste du programme ne connaît que l'interface `input.FactSource`.

### Entrées attendues

Un dossier contenant trois fichiers CSV de même préfixe, séparés par `;`, en UTF-8 :
`<préfixe>-classes.csv`, `<préfixe>-methods.csv`, `<préfixe>-calls.csv` (format décrit dans `resources/README.txt`). Le préfixe est déduit du dossier, ou donné avec `--prefix`.

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

### Dépendances

Aucune à l'exécution. JUnit Jupiter 5.14.4 pour les tests (téléchargé par Maven). Graphviz est facultatif, seulement pour transformer les `.dot` en images.

### Limites connues

- Une seule relation comptée : l'appel de méthode (héritage, interfaces, `new`, exceptions ignorés, comme imposé).
- Une coupe qui viole `M/2` est signalée, jamais corrigée.
- Pas d'adaptateur pour le modèle de faits du TP 1 (chemin 1) ; aucun bonus traité.

## 2. Environnement

| Outil | Version utilisée |
|---|---|
| JDK | Temurin 21.0.5 (le projet compile en Java 17, `release 17`) |
| Maven | 3.9.9 |
| JUnit Jupiter | 5.14.4 |
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

`mvn clean package` compile, exécute les 56 tests et produit `target/hai913i-tp2.jar`. Pour les tests seuls : `mvn clean test`.

## 4. Exécution

Depuis `starter/` :

```sh
java -jar target/hai913i-tp2.jar <dossier-des-faits> [--cp v1,v2,…] [--prefix p] [--out dossier]
```

| Argument | Rôle | Défaut |
|---|---|---|
| `<dossier-des-faits>` | dossier contenant les trois CSV | obligatoire |
| `--cp` | un ou plusieurs seuils de couplage interne moyen, séparés par des virgules, positifs ou nuls | `0.025` |
| `--prefix` | préfixe des fichiers (`facts-validation`…) | déduit du dossier |
| `--out` | dossier des exports, créé s'il n'existe pas | `sorties` |

Exemples :

```sh
# valeur retenue
java -jar target/hai913i-tp2.jar ../resources --cp 0.025

# plusieurs seuils, dont deux qui violent M/2, avec un tableau récapitulatif
java -jar target/hai913i-tp2.jar ../resources --cp 0.015,0.025,0.035,0.045

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

Contrôles automatiques : `mvn clean test` (56 tests). Déterminisme : deux exécutions donnent une sortie identique octet par octet :

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
| `Écriture des exports impossible` (code 2) | `--out` désigne un fichier, ou un dossier sans droit d'écriture. |
| Accents illisibles dans la console Windows | Passer la console en UTF-8 : `chcp 65001`. |
| `dot: command not found` | Graphviz absent : l'installer, ou ouvrir les `.dot` dans un visualiseur en ligne. |
| Contrainte `M/2` « VIOLÉE » | Ce n'est pas une erreur : le seuil CP est trop élevé pour ce projet ; baisser CP. |
