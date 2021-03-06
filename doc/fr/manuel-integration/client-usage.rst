Utilisation des clients externes
################################

Clients externes
================

Pour faciliter l'accès aux API externes, le projet VITAM met à disposition les clients externes Java correspondant.

.. tip:: Le code d'ihm-demo est un bon exemple d'utilisation des clients présentés ci-dessous.


Client Ingest
-------------

Le client Java des API ingest externes a les coordonnées maven suivantes :

.. code-block:: xml

	<dependency>
		<groupId>fr.gouv.vitam</groupId>
		<artifactId>ingest-external-client</artifactId>
		<version>${vitam.version}</version>
	</dependency>

La configuration du client est à réaliser conformément au paragraphe `Configuration d'un client externe`_ ; le fichier de configuration dédié à l'API d'ingest externe est le fichier ``ingest-external-client.conf`` :

.. literalinclude:: ../../../deployment/ansible-vitam/roles/vitam/templates/ihm-demo/ingest-external-client.conf.j2
   :language: yaml
   :linenos:



Une instance de client se récupère grâce au code suivant :

.. code-block:: java

	import fr.gouv.vitam.ingest.external.client
	IngestExternalClient client = IngestExternalClientFactorty.getInstance().getClient()

Pour la suite, se référer à la javadoc de la classe ``IngestExternalClient``.


Client Access
-------------

Le client Java des API access externes a les coordonnées maven suivantes :

.. code-block:: xml

		<dependency>
			<groupId>fr.gouv.vitam</groupId>
			<artifactId>access-external-client</artifactId>
			<version>${vitam.version}</version>
		</dependency>

La configuration du client est à réaliser conformément au paragraphe `Configuration d'un client externe`_ ; le fichier de configuration dédié à l'API d'access externe est le fichier ``access-external-client.conf`` :

.. literalinclude:: ../../../deployment/ansible-vitam/roles/vitam/templates/ihm-demo/access-external-client.conf.j2
   :language: yaml
   :linenos:


Une instance de client se récupère grâce au code suivant :

.. code-block:: java

	fr.gouv.vitam.access.external.client
	AccessExternalClient client = AccessExternalClientFactorty.getInstance().getClient()

Pour la suite, se référer à la javadoc de la classe ``AccessExternalClient``.



Configuration d'un client externe
---------------------------------

La configuration du client prend en compte les paramètres et fichiers suivants :

* La propriété système Java ``vitam.conf.dir`` : elle indique le répertoire dans laquelle les fichiers de configuration des clients seront recherchés (ex de déclaration en ligne de commande: ``-Dvitam.config.folder=/vitam/conf/clientvitam``) ;
* Le fichier de configuration (``<api>-client.conf``) : il doit être présent dans le répertoire défini précédemment ; c'est un fichier de configuration qui contient notamment les éléments de configuration suivants :

	- ``serverHost`` et ``serverPort`` permettent d'indiquer l'hôte et le port du serveur hébergeant l'API externe ;
	- keystore : ``keyPath`` et ``keyPassword`` permettent d'indiquer le chemin et le mot de passe du magasin de certificats contenant le certificat client utilisé par le client externe pour s'authentifier auprès de l'API externe ;
	- trusstore : ``keyPath`` et ``keyPassword`` permettent d'indiquer le chemin et le mot de passe du magasin de certificats contenant les certificats des autorités de certification requise (i.e. AC des certificats client et serveur).

