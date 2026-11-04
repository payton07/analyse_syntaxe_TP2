HAI913I - TP 2 - Compréhension et architectures (2026-2027)
Jeu de données canonique : les faits du projet de validation du TP 1
===================================================================

À QUOI ÇA SERT
--------------
Le TP 2 part des FAITS produits par le TP 1 : classes, méthodes, et graphe
d'appel.
Deux chemins sont possibles, et ils sont équivalents pour l'évaluation :

  CHEMIN 1 (recommandé) - vous branchez votre propre modèle de faits du TP 1
      derrière une petite interface d'adaptation, et le reste du TP 2 ne voit
      que cette interface.

  CHEMIN 2 (repli)      - vous chargez le jeu de données ci-dessous, qui
      contient les mêmes faits, valides, pour le projet de validation.

Le chemin 2 ne contient AUCUN code d'extraction : ni analyseur, ni AST, ni parcours de sources.


CE QUE CONTIENNENT LES FICHIERS
-------------------------------
facts-validation.json           tout le jeu de données, en un seul fichier
facts-validation-classes.csv    une ligne par classe        (séparateur : ;)
facts-validation-methods.csv    une ligne par méthode       (séparateur : ;)
facts-validation-calls.csv      une ligne par arc d'appel   (séparateur : ;)

Les deux formats (JSON et CSV) portent exactement les mêmes faits.
Choisissez celui que vous préférez ; le squelette fourni (starter/) lit les CSV, sans aucune dépendance.


LES CHIFFRES DU JEU DE DONNÉES
------------------------------
  classes              15    tous les types comptés par le TP 1
  méthodes             59    toutes les méthodes comptées par le TP 1
  callArcs             44    arcs du graphe d'appel (couples méthode -> méthode)
  callSitesTotal       46    sites d'appel correspondants
  interClassCallArcs   37    arcs dont les deux extrémités sont dans deux
                             classes DIFFÉRENTES  <-- le dénominateur du TP 2
  interClassCallSites  38    sites d'appel correspondants (convention alternative)

Ces chiffres sont ceux du point de contrôle B3 du TP 1 et de l'exemple filé du
CM 03.
Retenez la différence entre un ARC (un couple méthode appelante / méthode appelée, compté une fois) et un SITE D'APPEL (chaque occurrence dans le code).
Le TP 2 compte des ARCS.


SCHÉMA DU FICHIER JSON
----------------------
{
  "schema"      : "hai913i-tp2-facts/1",
  "project"     : "validation",
  "counts"      : { ... les six compteurs ci-dessus ... },

  "classes"     : [ { "name"       : nom qualifié, p. ex. "library.model.Item",
                      "simpleName" : "Item",
                      "package"    : "library.model" }, ... ],

  "methods"     : [ { "id"    : "library.model.Item#getTitle()",
                      "owner" : "library.model.Item" }, ... ],

  "calls"       : [ { "caller" : identifiant de la méthode appelante,
                      "callee" : identifiant de la méthode appelée,
                      "sites"  : nombre de sites d'appel pour ce couple }, ... ],

  "extra"       : { "inheritance" : [ ... ] }   FACULTATIF - voir plus bas
}

Identifiant d'une méthode : <nom qualifié de la classe>#<nom>(<types des paramètres séparés par des virgules>).
C'est exactement la convention du TP 1.
La classe propriétaire d'une méthode est donc aussi lisible dans son identifiant, avant le "#" ; le champ "owner" est là pour vous éviter de découper des chaînes.


SCHÉMA DES FICHIERS CSV
-----------------------
facts-validation-classes.csv
    class ; simpleName ; package

facts-validation-methods.csv
    method ; owner

facts-validation-calls.csv
    caller ; callerClass ; callee ; calleeClass ; sites

Les trois fichiers ont une ligne d'en-tête.
Le séparateur est le point-virgule ";".
Aucun champ ne contient de point-virgule, de guillemet ni de saut de ligne, donc un simple split(";") suffit.
Encodage UTF-8, fins de ligne CRLF.

Le fichier des classes est nécessaire : deux classes du projet n'apparaissent dans AUCUN appel, et elles doivent pourtant compter dans M (le nombre de classes) et apparaître dans le résultat.
Ne déduisez pas la liste des classes du seul fichier des appels.


LE BLOC "extra" (FACULTATIF)
----------------------------
"extra.inheritance" donne, pour chaque type qui en a, ses superclasses et ses
interfaces.
Ce bloc n'est là que pour le bonus C2, qui enrichit la relation de
dépendance et compare les modules obtenus.


CE QUE CE JEU DE DONNÉES NE CONTIENT PAS
----------------------------------------
Aucun couplage, aucune matrice, aucun regroupement, aucun dendrogramme, aucun module.
Ce sont les objets que le TP 2 vous demande de construire.

Il ne contient pas non plus les instanciations (new), les types d'attributs, les types de paramètres ni les levées d'exception.
La relation de dépendance imposée par le TP 2 ne compte que les appels de méthodes.
Les conséquences de ce choix sont visibles dans le résultat, et vous devez les discuter.


VÉRIFICATION RAPIDE
-------------------
Après chargement, votre programme doit annoncer :
    15 classes, 59 méthodes, 44 arcs d'appel,
    dont 7 arcs internes à une classe et 37 arcs inter-classes.
