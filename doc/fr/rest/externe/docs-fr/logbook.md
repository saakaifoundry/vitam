
*L'API de Journalisation* propose les points d'entrées et les méthodes pour requêter et récupérer les informations des **Journaux**.

*Notes: actuellement la base URI est **access-external/{version}**, cependant, il pourra être envisagé de séparer les ressources dans un autre service. On pourrait envisager par exemple un point d'entrée logbooks-external/{version} en plus du access-externak/{version}, afin de faciliter les accès.*

# API LogBook externe

les API externe dans le projet Vitam supportent le POST-X-HTTP-OVERRIDE=GET par contre au niveau des API interne ils supportent seulement le GET .

# Operations

**operations** est le point d'entrée pour tous les journaux des opérations réalisées dans Vitam (Journal du Service d'Archivage Electronique).

Le rôle du journal d'opération est de conserver une trace des opérations réalisées au sein du système lors de traitements sur des lots d'archives.

Chaque étape au sein d'une opération est subdivisée sous la forme de 2 enregistrements (début et fin) pour l'opération concernée.

Évènements tracés par exemple :

* Démarrage de Ingest avec affectation d'un eventIdentifierProcess (OperationId)
* Stockage du lot d'archives dans l'espace de travail
* Démarrage d'un workflow
* Démarrage d'une étape de workflow
* Fin d'une étape de workflow
* Fin d'un workflow
* Fin du Stockage du lot
* Fin de Ingest

# UnitLifeCycles

**unitlifecycles** est le point d'entrée pour tous les journaux de cycle de vie des units dans Vitam.
Le rôle des journaux de cycles de vie des units est de conserver l'ensemble des événements associés à une Unit.
Les événements associés sont du type :
- création
- modification de métadonnées
- élimination
- audit
- gel
- ...

**Important** : l'identifiant d'un Unit lifecycle est également l'identifiant du Unit correspondant dans le service Access.
Ainsi il est possible une fois une opération terminée de demander la liste des Units qui sont concernés par cette opération en demandant la liste des UnitLifeCycles de cette opération. Les identifiants remontés sont alors les mêmes pour accéder au journal du cycle de vie d'une Unit (*/unitlifecycles/id*) ou à sa description et ses métadonnées de gestion (*/units/id*).   

# ObjectLifeCycles

**objectlifecycles** est le point d'entrée pour tous les journaux de cycle de vie des objets dans Vitam.
Le rôle des journaux de cycles de vie des objets est de conserver l'ensemble des événements associés à un Objet.
Les événements associés sont du type :
- création
- check de conformité (empreinte, format, taille)
- transformation de format
- élimination
- audit
- gel
- ...


**Important** : l'identifiant d'un Object lifecycle est également l'identifiant de l'Objet correspondant dans le service Access.
Ainsi il est possible une fois une opération terminée de demander la liste des Objects qui sont concernés par cette opération en demandant la liste des ObjectLifeCycles de cette opération. Les identifiants remontés sont alors les mêmes pour accéder au journal du cycle de vie d'un Object (*/objectlifecycles/id*) ou à sa description technique (*/objects/id*).   



# Sécurisation des journaux

**traceability** est le point d'entrée pour la sécurisation des journaux d'opérations dans Vitam.

Il est possible de gérer la sécurisation des journaux et de vérifier a posteriori leur conformité.

**NOTE IMPORTANTE** : **DEPRECATED** Ce service sera déplacé dans la partie administration fonctionnelle.
