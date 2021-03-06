Principes
#########

Les principes de sécurité de VITAM suivent les directives suivantes :

* Authentification et autorisation systématique des systèmes clients de VITAM basées sur une authentification TLS mutuelle utilisant des certificats (pour les composants de la couche accès) ;
* Validation systématique des entrées du système :

    - Détection et suppression de codes malveillants dans les archives déposées dans VITAM ;
    - Robustesse contre les failles du Top Ten OWASP pour toutes les interfaces REST ;

* Validation périodique des listes de CRL pour toutes les CA trustées par VITAM.


Principes de cloisonnement
==========================

Les principes de cloisonnement en zones, et notamment les implications en termes de communication entre ces zones ont été décrits dans :doc:`la section dédiée aux principes de déploiement </archi-exploit-infra/principes/30-principles-deployment>`.


Principes de sécurisation des accès externes
============================================

Les services logiciels en contact direct avec les clients du SAE (i.e. les services ``*-external``) implémentent les mesures de sécurité suivantes :

* Chiffrement du transport des données entre les applications externes et VITAM via HTTPS ; par défaut, la configuration suivante est appliquée :

    - Protocoles exclus : ``SSLv2``, ``SSLv3``
    - Ciphers exclus : ``.*NULL.*``, ``.*RC4.*``, ``.*MD5.*``, ``.*DES.*``, ``.*DSS.*``

.. note:: Dans cette version du système VITAM, les ciphers recommandés sont : ``TLS_ECDHE.*``, ``TLS_DHE_RSA.*``

* Authentification par certificat x509 requise des applications externes (authentification M2M) basée sur une liste blanche de certificats valides ;

    - Lors d’une connexion, la vérification synchrone confirme que le certificat proposé n’est pas expiré (not before, not after) et est bien présent dans le référentiel d’authentification des certificats valides (qui est un fichier keystore contenant la liste des certificats valides).

.. note:: Dans cette version du système VITAM, la liste des certificats reconnus est stockée dans un keystore Java.

* Filtrage exhaustif des données et requêtes entrant dans le système basé sur :

    - Un WAF applicatif permettant le filtrage d'entrées pouvant être une menace pour le système (intégration de la bibliothèque `ESAPI <https://www.owasp.org/index.php/Category:OWASP_Enterprise_Security_API>`_ protégeant notamment contre les attaques de type XSS)
    - Support de l'utilisation d'un ou plusieurs antivirus (configurables et extensibles) dans le composant d'entrée (``ingest``) permettant de valider l'inocuité des données entrantes.

.. note:: Dans cette version du système, le paramétrage de l'antivirus est supporté lors de l'installation, mais pas le paramétrage d'ESAPI (notamment les filtres appliqués).


Principes de sécurisation des communications internes au système
================================================================

Le secret de plateforme permet de se protéger contre des erreurs de manipulation et de configuration en séparant les environnements de manière logique (secret partagé par l'ensemble de la plateforme mais différent entre plateformes). Ce secret (chaîne de caractères) est positionné dans la configuration des composants lors de l'installation du système.

Dans chaque requête, les deux headers suivants sont positionnés :

* ``X-Request-Timestamp`` : il contient le timestamp de la requête sous forme epoch (secondes depuis 1970)
* ``X-Platform-ID`` : il contient la valeur suivante : SHA256("<methode>;<URL>;<Valeur du header X-Request-Timestamp>;<Secret partagé de plateforme>")

Du côté du composant cible de la requête, le contrôle est alors le suivant :

* Existence des deux headers précédents ;
* Vérification que timestamp envoyé est distant de l'heure actuelle sur le serveur requêté de moins de 10 secondes ( ``| Timestamp - temps local | < 10 s`` )
* Validation du hash transmis via la réalisation du même calul sur le serveur cible et de la comparaison des résultats.

En cas d'échec d'une de ces validations, la requête est refusée.

.. note:: Les headers et le body de la requête ne sont pas inclus dans le calcul du X-Platform-ID pour des raisons de performance.


Principes de sécurisation des bases de données
==============================================

Les bases de données sont sécurisées via un cloisonnement physique et/ou logique des différentes bases de données qui les constituent.

MongoDB
-------

Dans le cas de MongoDB, le cloisonnement est logique. Chaque service hébergeant des données dans MongoDB se voit attribuer une base et un utilisateur dédié. Cet utilisateur a uniquement les droits de lecture / écriture dans les collections de cette base de données, mais ne peut notamment pas modifier la structure des collections de sa base de données ni accéder aux collections d'une autre base de données.

Un utilisateur technique "root" est également créé pour les besoins de l'installation et de la configuration de MongoDB.

Chaque base de données ne doit être accédée que par les instances d'un seul service (ex: le service logbook est le seul à accéder à la base de données logbook).

Enfin, l'accès anonyme à MongoDB est désactivé, et les utilisateurs sont authentifiés par le couple utilisateur / mot de passe.


Elasticsearch
-------------

Dans le cas d'Elasticsearch, le cloisonnement est principalement physique, dans le sens où le cluster hébergeant les données métier est disjoint du cluster hébergeant les données techniques.

.. caution:: L'accès au cluster Elasticsearch est anonyme sans authentification requise ; ceci est dû à une limitation de la version OpenSource d'Elasticsearch, et pourra être réévalué dans les futures versions du système VITAM.


Principes de sécurisation des secrets de déploiement
====================================================

Les secrets de l'intégralité de la solution VITAM déployée sont tous présents sur le serveur de déploiement ; par conséquent, ils doivent y être stockés de manière sécurisée, avec les principes suivants :

* Les mot de passe et token utilisés par ansible doivent être stockés dans des fichiers d'inventaire chiffrés par ansible-vault ;
* Les clé privées des certificats doivent être protégées par des mot de passe complexes ; ces derniers doivent suivre la règle précédente.


