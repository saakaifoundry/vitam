Exemple DSL Vitam
#################

Cette partie va essayer de montrer quelques exemples d'usages du DSL dans différentes conditions.

Collection Units
================

**Points particuliers sur les end points**

- **/units** : il s'agit ici de requêter un ensemble d'archives (Units) sur leurs métadonnées. Bien que non encore supportée, il sera possible de réaliser des UPDATE massifs sur cet end-point.

  - **$root** peut être vide ou renseigné : il sera contrôlé via les contrats d'accès associés à l'application.

    - S'il est vide, il prendra les valeurs renseignées par les contrats.
    - S'il contient des identifiants, il sera vérifié que ces identifiants sont bien soient ceux des contrats, soient fils de ceux spécifiés dans ces contrats.

  - Le résultat doit être une liste de Units (vide ou pas)

- **/units/id** : il s'agit ici de requêter depuis un Unit donné. Le résultat peut être multiple selon les query spécifiées (et notamment le *$depth*).

  - **$roots** est implicite à la valeur de id (si une valeur est spécifiée, elle sera ignorée)

    - Cet Id sera toujours contrôlé par rapport aux contrats d'accès associés à l'application.

  - Le résultat doit être une liste de Units (vide ou pas)

- **/units/id/object** : il s'agit ici d'accéder, s'il existe, à l'objet (ObjectGroup) associé à cet Unit.

  - Les query peuvent remonter les métadonnées (Header **Accept: application/json**)
  - Les query peuvent remonter un des objets binaires (Headers **Accept: application/octet-stream** et **X-Qualifier** et **X-Version**)
  - Le résultat doit être une liste de Objects (pour application/json) ou d'un seul objet binaire (pour application/octet-stream)

Collection Objects
==================

**Points particuliers sur les end points**
**Cette collection est DEPRECATED et va disparaître car elle est contraire aux règles d'accès aux objets à partir d'une ArchiveUnit (/units/id/object).**

- **/objects** : il s'agit ici de requêter un ensemble d'objets sur leurs métadonnées uniquement.

  - **$root** peut être vide ou renseigné : il sera contrôlé via les contrats d'accès associés à l'application et ne concerne que des Id de Units.

    - S'il est vide, il prendra les valeurs renseignées par les contrats (Id de Units parentes).
    - S'il contient des identifiants, il sera vérifié que ces identifiants sont bien soient ceux des contrats, soient fils de ceux spécifiés dans ces contrats (toujours des Id de Units).

  - Le résultat doit être une liste de Objects (vide ou pas)
  - Cette fonction est surtout utile pour des données statistiques sur les objets dans leur ensemble.

- **/objects/id** : **(susceptible d'être dépréciée dans une prochaine version)** il s'agit ici d'accéder à un objet (ObjectGroup).

  - Les query peuvent remonter les métadonnées (Header **Accept: application/json**)
  - Les query peuvent remonter un des objets binaires (Headers **Accept: application/octet-stream** et **X-Qualifier** et **X-Version**)
  - Le résultat doit être une liste de Objects (pour application/json) ou d'un seul objet binaire (pour application/octet-stream)

Exemples d'usages du DSL
========================

Partie $query
-------------

Rappel sur l'usage de $depth
****************************

- $query peut contenir plusieurs Query, qui seront exécutées successivement (tableau de Query).
- Une Query correspond à la formulation "*WHERE xxx*" dans le langage SQL, c'est à dire les critères de sélection.
- La succession est exécutée avec la signification suivante :

  - Depuis $roots, chercher les Units/Objects tel que Query[1], conduisant à obtenir une liste d'identifiants[1]
  - Cette liste d'identifiants[1] devient le nouveau $roots, chercher les Units/Objects tel que Query[2], conduisant à obtenir une liste d'identifiants[2]
  - Et ainsi de suite, la liste d'identifiants[n] de la dernière Query[n] est la liste de résultat définitive sur laquelle l'opération effective sera réalisée (SELECT, UPDATE, INSERT, DELETE) selon ce que l'API supporte (GET, PUT, POST, DELETE).
  - Chaque query peut spécifier une profondeur où appliquer la recherche :

    - *$depth = 0* : sur les items spécifiés (filtre sur les mêmes items, à savoir pour la première requête ceux de $roots, pour les suivantes, le résultat de la requête précédente, c'est à dire le nouveau $roots)
    - *$depth < 0* : sur les items parents (hors les items spécifiés dans le $roots courant)
    - *$depth > 0* : sur les items enfants (hors les items spécifiés dans le $roots courant)
      - **par défaut, $depth vaut 1** (enfants immédiats dans le $roots courant)

  - Le principe est résumé dans le graphe d'états suivant :

.. image:: images/multi-query-schema.png


- $source (**UNSUPPORTED**) permet de changer de collections entre deux query (unit ou object)


Détails sur la partie $filter $orderby
**************************************

Il y a une différence entre un tri sur un champ non analysé (date, nombre, code) et un champ analysé :

- Pour un champ non analysé : l'ordre est lexicographique pour un texte, l'ordre est naturel pour un champ date ou nombre
- **IMPORTANT** : Pour un champ analysé (plein texte), le tri n'est pas lexicographique mais basé sur le score de correspondance
- l'ordre de déclaration des tris est respectés dans la réponse

Détails sur chaque commande de la partie $query
***********************************************

- $path : [ id1, id2, ... ]

  - Accès direct à un ou plusieurs noeuds
  - **$path : [ "id1", "id2" ]** est l'équivalent implicite de *$in : { #id : [ id1, id2 ] }* mais sur le champ #id uniquement
  - **Important** : cette commande n'est autorisée qu'en première position. Elle implique une vérification que les *$roots* sont compatibles avec ces Ids qui deviennent les nouveaux $roots implicitement

::

   { "$path" : [ "id1", "id2" ] }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = path("id1", "id2");


- $and, $or, $not

  - Combinaison logique d'opérateurs
  - **$and : [ expression1, expression2, ... ]** où chaque expression est une commande et chaque commande doit être vérifiée
  - **$or** où chaque expression est une commande et au moins une commande doit être vérifiée
  - **$not** où chaque expression est une commande et aucune ne doit être vérifiée
  - Exemple :

::

   { "$and" : [ { "$gt" : { "StartDate" : "2014-03-25" } }, { "$lte" : { "StartDate" : "2014-04-25" } } ] }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = and().add(gt("StartDate", dateFormat.parse("2014-03-25")), 
            lte("StartDate", dateFormat.parse("2014-04-25"));

pour toute StartDate plus grande que le 25 mars 2014 et inférieure ou égale au 25 avril 2014 (équivalent à un $range dans ce cas)

- $eq, $ne, $lt, $lte, $gt, $gte

  - Comparaison de la valeur d'un champ et la valeur passée en argument
  - **$gt : { name : value }** où *name* est le nom du champ et *value* la valeur avec laquelle on compare le champ

    - $eq : égalité, marche également avec les champs non analysés (codes). **Attention** : pour les champs analysés, il s'agit d'un $match_all.
    - $ne : le champ n'a pas la valeur dournie
    - $lt, $lte : le champs a une valeur inférieure ou égale avec la valeur fournie
    - $gt, $gte : le champs a une valeur supérieure ou égale avec la valeur fournie

- Exemple :


::

   { "$gt" : { "StartDate" : "2014-03-25" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = gt("StartDate", dateFormat.parse("2014-03-25"));

pour toute StartDate plus grande que le 25 mars 2014

- $range

  - Comparaison de la valeur d'un champ avec l'intervalle passé en argument
  - **$range : { name : { $gte : value, $lte : value } }** est un raccourci pour chercher sur un seul champ nommé *name* les Units dont la valeur est comprise entre la partie *$gt* ou *$gte* et la partie *$lt* ou *$lte*
  - Exemple :

::

   { "$range" : { "StartDate" : { "$gte" : "2014-03-25", "$lte" : "2014-04-25" } } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = range("StartDate", dateFormat.parse("2014-03-25"), true, 
         dateFormat.parse("2014-04-25"), true);

pour toute StartDate plus grande ou égale au 25 mars 2014 mais inférieure ou égale au 25 avril 2014

- $exists, $missing, $isNull
   - Existence d'un champ
   - **$exists : name** où *name* est le nom du champ qui doit exister
   - **$missing** : le champ ne doit pas exister
   - **$isNull** : le champ existe mais vide
   - Exemple :

::

   { "$exists" : "StartDate" }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = exists("StartDate");

pour tout Unit contenant le champ StartDate

- $in, $nin
   - Présence de valeurs dans un champ (ce champ peut être un tableau ou un simple champ avec une seule valeur)
   - **$in : { name : [ value1, value2, ... ] }** où *name* est le nom du tableau et le tableau de valeurs ce que peut contenir le tableau. Il suffit d'une seule valeur présente dans le tableau pour qu'il soit sélectionné.
   
     - **Attention** : pour les champs analysés, il s'agit d'un $match multiple via $or.
   
   - **$nin** est l'opérateur inverse, le tableau ne doit contenir aucune des valeurs spécifiées
   - Exemple :

::

   { "$in" : { ""#unitups" : ["id1", "id2"] } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = in(unitups(), "id1", "id2");

pour rechercher les Units qui ont pour parents immédiats au moins l'un des deux Id spécifiés

- $size
   - Taille d'un tableau
   - **$size : { name : length }** où *name* est le nom du tableau et *length* la taille attendue (égalité)
   - Exemple :

::

   { "$size" : { ""#unitups" : 2 } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = size(unitups(), 2);

pour rechercher les Units qui ont 2 parents immédiats exactement

- $term

  - Comparaison de champs avec une valeur exacte (non analysé)
  - **$term : { name : term, name : term }** où l'on fait une recherche exacte sur les différents champs indiqués
  - **Attention** : pour les champs analysés, il s'agit d'un $match_all.
  - Exemple :

::

   { "$term" : { "#id" : "guid" } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = term(id(), guid);

qui cherchera le Unit ayant pour Id celui précisé (équivalent dans ce cas à $eq) (non analysé, donc pour les codes uniquement)

- $wildcard

  - Comparaison de champs mots-clefs à valeur
  - **$wildcard : { name : term }** où l'on fait une recherche exacte sur le champ indiqué mais avec une possibilité d'introduire un '\*' dans le contenu
  - **NOTA BENE** : cette requête est coûteuse.
  - Exemple :

::

   { "$wildcard" : { "#type" : "FAC*01" } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = wildcard(type(), "FAC*01");

qui cherchera les Units qui contiennent dans le type (Document Type) une valeur commençant par FAC et terminant par 01 (non analysé, donc pour les codes uniquement)

- $match, $match\_all, $match\_phrase, $match\_phrase\_prefix

  - Recherche plein texte soit sur des mots, des phrases ou un préfixe de phrase
  - **$match : { name : words, $max\_expansions : n }** où *name* est le nom du champ, *words* les mots que l'on cherche, dans n'importe quel ordre, et optionnellement *n* indiquant une extension des mots recherchés ("seul" avec n=5 permet de trouver "seulement")
  - **$match\_all : { name : words, $max\_expansions : n }** où *name* est le nom du champ, *words* les mots que l'on cherche (tous), dans n'importe quel ordre, et optionnellement *n* indiquant une extension des mots recherchés ("seul" avec n=5 permet de trouver "seulement")
  - **$match\_phrase** permet de définir une phrase (*words* constitue une phrase à trouver exactement dans cet ordre)
  - **$match\_phrase\_prefix** permet de définir que le champ *name* doit commencer par cette phrase
  - **NOTA BENE** : dans le cas de champs non analysés, cette requête est remplacé par une requête de type "prefix".
  - Exemple :

::

   { "$match" : { "Title" : "Napoléon Waterloo" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = match("Title", "Napoléon Waterloo");

qui cherchera les Units qui contiennent les deux mots dans n'importe quel ordre dans le titre

::

   { "$match_phrase" : { "Description" : "le petit chat est mort" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = matchPhrase("Description", "le petit chat est mort");

qui cherchera les Units qui contiennent la phrase n'importe où dans la description

- $regex

  - Recherche via une expression régulière
  - **NOTA BENE** : cette requête est très lenbte et très coûteuse.
  - **$regex : { name : regex }** où *name* est le nom du champ et *regex* l'expression au format expression régulière du contenu du champ
  - Exemple :

::

   { "$regex" : { "Title" : "Napoléon.\* [Waterloo | Leipzig]" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = regex("Title", "Napoléon.\* [Waterloo | Leipzig]");

qui cherchera les Units qui contiennent exactement Napoléon suivi de n'importe quoi mais se terminant sur un choix parmi Waterloo ou Leipzig dans le titre

- $search

  - Recherche du type moteur de recherche
  - **$search : { name : searchParameter }** où *name* est le nom du champ, *searchParameter* est une expression de recherche
  - L'expression est formulée avec les opérateurs suivants :

    - **+** signifie AND
    - **|** signifie OR
    - **-** empêche le mot qui lui est accollé (tout sauf ce mot)
    - **"** permet d'exprimer un ensemble de mots en une phrase (l'ordre des mots est impératif dans la recherche)
    - **\*** A la fin d'un mot signifie que l'on recherche tout ce qui contient un mot commençant par
    - **(** et **)** signifie une précédence dans les opérateurs (priorisation des recherches AND, OR)
    - **~N** après un mot est proche du **\*** mais en limitant le nombre de caractères dans la complétion (fuzziness)
    - **~N** après une phrase (encadré par **"**) autorise des "trous" dans la phrase
    - **Attention** : pour les champs non analysés, il s'agit d'un $term multivalué (choix parmi plusieurs valeurs).
  - Exemple :

::

   { "$search" : { "Title" : "\"oeufs cuits\" +(tomate | patate) -frite" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = search("Title", "\"oeufs cuits\" +(tomate | patate) -frite");

pour rechercher les Units qui ont dans le titre la phrase "oeufs cuits" et au moins un parmi tomate ou patate, mais pas frite

- $flt, $mlt

  - Recherche « More Like This », soit par valeurs approchées
  - **$mlt : { $fields : [ name1, name2 ], $like : like\_text }** où *name1*, *name2*, ... sont les noms des champs concernés, et *like_text* un champ texte avec lequel on va comparer les différents champs fournies pour trouver des éléments "ressemblant" à la valeur fournie (il s'agit d'une recherche permettant de chercher quelque chose qui ressemble à la valeur fournie, pas l'égalité, en mode plein texte)

    - $mlt : More like this, la méthode recommandée
    - $fmt : Fuzzy like this, une autre que fournie l'indexeur mais pouvant donner plus de faux positif et qui est un assemblage de $match avec une combinaison "$or"

  - Exemple :

::

   { "$mlt" : { "$fields" : ["Title", "Description"], "$like" : "Il était une fois" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = mlt("Il était une fois", "Title", "Description");

pour chercher les Units qui ont dans le titre ou la description un contenu qui s'approche de la phrase spécifiée dans $like.


Partie $action dans la fonction Update
--------------------------------------

- $set

  - change la valeur des champs
  - **$set : { name1 : value1, name2 : value2, ... }** où *nameX* est le nom des champs à changer avec la valeur indiquée dans *valueX*
  - **NOTA BENE**: $set admet maintenant une liste de valeur pour un champ de type tableau.
  - Exemple :

::

   { "$set" : { "Title" : "Mon nouveau titre", "Description" : "Ma nouvelle description" }" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = set("Title", "Mon nouveau titre").add("Description", "Ma nouvelle description");

qui change les champs Title et Description avec les valeurs indiquées

- $unset

  - enlève la valeur des champs
  - **$unset : [ name1, name2, ... ]** où *nameX* est le nom des champs pour lesquels on va supprimer les valeurs

    - Exemple :

::

   { "$unset" : [ "StartDate", "EndDate" ]" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = unset("StartDate", "EndDate");

qui va vider les champs indiqués de toutes valeurs

- $min, $max

  - change la valeur du champ à la valeur minimale/maximale si elle est supérieure/inférieure à la valeur précisée
  - **$min : { name : value }** où *name* est le nom du champ où si sa valeur actuelle est inférieure à *value*, sa valeur sera remplacée par celle-ci
  - **$max** idem en sens inverse, la valeur sera remplacée si l'existante est supérieure à celle indiquée
  - Exemple :

::

   { "$min" : { "MonChamp" : 3 }" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = set("Title", "Mon nouveau titre").add("Description", "Ma nouvelle description");

Si MonCompteur contient 2, MonCompteur vaudra 3, mais si MonCompteur contient 4, la valeur restera inchangée

- $inc

  - incrémente/décremente la valeur du champ selon la valeur indiquée
  - **$inc : { name : value }** où *name* est le nom du champ à incrémenter de la valeur *value* passée en paramètre (positive ou négative)
  - Exemple :

::

   { "$inc" : { "MonCompteur" : -2 }" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = inc("MonCompteur", -2);

décrémente de 2 la valeur initiale de MonCompteur

- $rename

  - change le nom du champ
  - **$rename : { name : newname }** où *name* est le nom du champ à renommer en *newname*
  - les champs préfixés par '#' ne peuvent pas être renommés.
  - Exemple :

::

   { "$rename" : { "MonChamp" : "MonNouveauChamp" }" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = rename("MonChamp", "MonNouveauChamp");

où le champ MonChamp va être renommé en MonNouveauChamp

- $push, $pull

  - ajoute en fin ou retire les éléments de la liste du champ (qui est un tableau)
  - **$push : { name : { $each : [ value, value, ... ] } }** où *name* est le nom du champ de la forme d'un tableau (une valeur peut apparaître plus dune seule fois dans le tableau) et les valeurs sont ajoutées à la fin du tableau
  - **$pull** a la même signification mais inverse, à savoir qu'elle enlève du tableau les valeurs précisées si elles existent
  - Exemple :

::

   { "$push" : { "Tag" : { "$each" : [ "Poisson", "Oiseau" ] } } }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = push("Tag", "Poisson", "Oiseau");

ajoute dans le champ Tag les valeurs précisées à la fin du tableau même si elles existent déjà dans le tableau

- $add

  - ajoute les éléments de la liste du champ (unicité des valeurs)
  - **$add : { name : { $each : [ value, value, ... ] } }** où *name* est le nom du champ de la forme d'une MAP ou SET (une valeur ne peut apparaître qu'une seule fois dans le tableau) et les valeurs sont ajoutées, si elles n'existent pas déjà
  - **$pull** peut être utilisé pour retirer une valeur
  - Exemple :

::

   { "$add" : { "Tag" : { "$each" : [ "Poisson", "Oiseau" ] } } }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = add("Tag", "Poisson", "Oiseau");

ajoute dans le champ Tag les valeurs précisées sauf si elles existent déjà dans le tableau

- $pop

  - ajoute ou retire un élément du tableau en première ou dernière position selon la valeur -1 ou 1
  - **$pop : { name : value }** où *name* est le nom du champ et si *value* vaut -1, retire le premier, si *value* vaut 1, retire le dernier
  - Exemple :

::

   { "$pop" : { "Tag" : -1 } }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = pop("Tag", -1);

retire dans le champ Tag la première valeur du tableau

Exemple d'un SELECT Multi-queries
=================================

::

   {
    "$roots": [ "id0" ],
    "$query": [
      { "$match": { "Title": "titre" }, "$depth": 4 },
      { "$and" : [ { "$gt" : { "StartDate" : "2014-03-25" } },
        { "$lte" : { "EndDate" : "2014-04-25" } } ], "$depth" : 0},
      { "$exists" : "FilePlanPosition" }
    ],
    "$filter": { "$limit": 100 },
    "$projection": { "$fields": { "#id": 1, "title": 1, "#type": 1, "#parents": 1, "#object": 1 } }
   }

   include fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   Query query1 = match("Title", "titre").setDepthLimit(4);
   Query query2 = and(gt("StartDate", dateFormat.parse("2014-03-25")), 
         lte("EndDate", dateFormat.parse("2014-04-25")))
         .setDepthLimit(0);
   Query query3 = exists("FilePlanPosition");
   SelectMultiQuery select = new SelectMultiQuery().addRoots("id0")
         .addQueries(query1, query2, query3)
         .setLimitFilter(0, 100)
         .addProjection(id(), "Title", type(), parents(), object());
   JsonNode json = select.getFinalSelect();

1. Cette requête commence avec le Unit id0. A partir de ce Unit, on cherche des Units qui sont fils avec une distance d'au plus 4 du noeud id0 et où Title contient "titre", ce qui donne une nouvelle liste d'Ids.
2. La query suivante utilise la liste d'Ids précédemment obtenue pour effectuer un filtre sur celle-ci ($depth = 0) et vérifie une condition sur StartDate et EndDate, ce qui donne une nouvelle liste d'Ids, sous-ensemble de celle obtenue en étape 1.
3. La query suivante utilise la liste d'Ids précédemment obtenue comme point de départ et cherche les fils immédiats ($depth = 1 implicite) qui vérifie la condition que FilePlanPosition, ce qui donne une nouvelle d'Ids.
4. Sur la base de cette nouvelle liste d'Ids obtenue de l'étape 3, seuls les 100 premiers sont retournés, et le contenu de ce qui est retourné est précisé dans la projection.

A noter qu'il aurait été possible d'optimiser cette requête comme suit :

::

   {
    "$roots": [ "id0" ],
    "$query": [
      { "$and" : [ { "$match": { "Title": "titre" } },
        { "$gt" : { "StartDate" : "2014-03-25" } },
        { "$lte" : { "EndDate" : "2014-04-25" } } ], "$depth" : 4},
      { "$exists" : "FilePlanPosition" }
    ],
    "$filter": { "$limit": 100 },
    "$projection": { "$fields": { "#id": 1, "title": 1, "#type": 1, "#parents": 1, "#object": 1 } }
   }

   include fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   Query query2 = and(match("Title", "titre"), gt("StartDate", dateFormat.parse("2014-03-25")), 
         lte("EndDate", dateFormat.parse("2014-04-25"))).setDepthLimit(4);
   Query query3 = exists("FilePlanPosition");
   SelectMultiQuery select = new SelectMultiQuery().addRoots("id0")
         .addQueries(query2, query3)
         .setLimitFilter(0, 100)
         .addProjection(id(), "Title", type(), parents(), object());
   JsonNode json = select.getFinalSelect();

Car la requête 1 et 2 sont unifiées en une seule.


Exemple de scénarios
====================

Cas du SIP Mercier.zip
----------------------

**Etape 1**

1. je cherche l'article 2 (ArchivalAgencyArchiveUnitIdentifier) = les discours prononcés devant l'Assemblée nationale

::

  {
    "$roots": [],
    "$query": [
          {
            "$match": {
              "Title": "assemblée"
            },
            "$depth": 20
          },
          {
            "$match": {
              "Title": "discours"
            },
            "$depth": 20
          }
        ]
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {

     }
    }
  }

**Etape 2**

2. je cherche les discours prononcés lors de la préparation de la loi relative au défenseur des droits, que ce soit à l'Assemblée nationale ou le Sénat (Title = défenseur)

::

  {
    "$roots": [],
    "$query": [
      {
        "$or": [
          {
            "$match": {
              "Title": "sénat"
            }
          },
          {
            "$match": {
              "Title": "assemblée"
            }
          }
        ],
        "$depth": 20
      },
      {
        "$and": [
          {
            "$match": {
              "Title": "défenseur"
            }
          }
        ],
        "$depth": 20
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {
      }
    }
  }


**Etape 3**

3. je cherche dans le dossier Sénat (Title = Sénat), les discours prononcés lors de la relative au défenseur des droits (Title = défenseur)

::

  {
    "$roots": [],
    "$query": [
      {
        "$and": [
          {
            "$eq": {
              "Title": "Sénat"
            }
          }
        ],
        "$depth": 20
      },
      {
        "$and": [
          {
            "$match": {
              "Title": "défenseur"
            }
          }
        ],
        "$depth": 20
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {
      }
    }
  }


**Etape 4**

4. je cherche les discours prononcé sur telle intervalle de date (StartDate, EndDate)

::

  {
    "$roots": [],
    "$query": [
        {
        "$or": [
          {
            "$match": {
              "Title": "discours"
            }
          }
        ],
        "$depth": 20
      },
      {
        "$and": [
          { "$range" : { "StartDate" : { "$gte" : "2012-10-22", "$lte" : "2012-11-07" } } },
          { "$range" : { "EndDate" : { "$gte" : "2012-11-07", "$lte" : "2012-11-08" } } }
        ],
        "$depth": 0
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {

     }
    }
  }


Cas du SIP 1069_OK_RULES_COMPLEXE_COMPLETE.zip
----------------------------------------------

**Etape 1**

1. je cherche l'AU dont le titre est Botzaris (Title = Botzaris)

::

  {
    "$roots": [],
    "$query": [
          {
            "$match": {
              "Title": "Botzaris"
            },
            "$depth": 20
          }
        ]
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {

     }
    }
  }


**Etape 2**

2. je cherche les AU qui ne seront pas communicables au 01/01/2018 (= les AU qui ont une AccesRule avec une EndDate postérieure au 01/01/2018)

::

  {
    "$roots": [],
    "$query": [
      {
        "$or": [
          {
            "$gt": {
              "#management.AccessRule.EndDate": "2018-01-01"
            }
          }
        ],
        "$depth": 0
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {
      	"#rules" : 1, "Title" : 1
      }
    }
  }


**Etape 3**

3. je cherche les AU qui ont une AppraisalRule avec sort final = Destroy

::

  {
    "$roots": [],
    "$query": [
      {
        "$or": [
          {
            "$eq": {
              "#management.AppraisalRule.FinalAction": "Destroy"
            }
          }
        ],
        "$depth": 0
      }
    ],
    "$filter": {
      "$orderby": {
        "TransactedDate": 1
      }
    },
    "$projection": {
      "$fields": {
      	"#rules" : 1, "Title" : 1
      }
    }
  }
