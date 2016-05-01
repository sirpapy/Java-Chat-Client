--------------------------------------------------------------------------------

Architecture du répertoire

.
	./classes : répertoire des classes java (crée par ANT init)
	./config : répertoire des fichiers de configuration
		./config/client.conf : fichier de configuration du client
		./config/server.conf : fichier de configuration du serveur
	./docs : répertoire de la documentation
		./docs/api : javadoc des sources (crée par ANT javadoc)
		./docs/rfc.txt : la RFC du projet
		./docs/dev.pdf : le manuel développeur du projet
		./docs/usr.pdf : le manuel utilisateur du projet
	./files : répertoire où seront rangés les fichiers reçus (crée par ANT init)
	./jar : répertoire des JAR exécutables pré-générés
		./jar/client.jar : JAR exécutable du client (pré-généré)
		./jar/server.jar : JAR exécutable du server (pré-généré)
	./src : répertoire des sources
	./build.xml : fichier ANT
	./README.txt : c'est ce que vous lisez actuellement

--------------------------------------------------------------------------------

Tâches ANT (par défaut : compile -> jar -> javadoc)

	init : créer les répertoires "./classes", "./docs/api" et "./files"
	clean : supprime le répertoire "./classes", le répertoire "./docs/api" et les JAR exécutables.
	compile (nécessite "init") : compile les sources
	jar (nécessite "compile") : génère les JAR exécutable à la racine du projet
	javadoc : génère la javadoc des sources

--------------------------------------------------------------------------------

Exécuter le serveur : java -jar [options] server.jar port
	port : numéro du port d'écoute du serveur
	
Exécuter le client : java -jar client.jar [options] hostname port [username]
	hostname : adresse ou nom d'hôte du serveur
	port : numéro du port d'écoute du serveur
	username : pseudo à utiliser (facultatif)
	
Options (serveur et client) :
	-logger path : rediriger la sortie normale du logger dans un fichier (au lieu du terminal)
	-exception path : rediriger la sortie des exception du logger dans un fichier (au lieu du terminal)
	
--------------------------------------------------------------------------------

Lancer un client de chat
	Si un pseudo a été indiqué à la fin de la ligne de commande, le client va essayer de se connecter au serveur avec ce pseudo. En cas d'echec, le client s'arrête immédiatement.
	Si aucun pseudo n'a été indiqué à la fin de la ligne de commande, l'utilisateur devra indiquer un pseudo manuellement sur le terminal afin que le client essaye de se connecter au serveur avec ce pseudo. En cas d'échec, le client attendra un nouveau pseudo jusqu'à recevoir un pseudo valid.
	Une fois connecté et authentifié, le client est identifié de manière unique par ce pseudo durant toute sa session.
	Le pseudo d'un client est libéré à sa déconnexion.
	
Les commandes de chat (une fois connecté et authentifié)
	<message> : envoyer un message
	/open <pseudo> : demander à ouvrir une connexion privé avec quelqu'un (il faut qu'il soit connecté actuellement)
	/accept <pseudo> : accepter d'ouvrir une connexion privée avec quelqu'un (il faut qu'il ait demandé la connexion au préalable)
	/pv <pseudo> <message> : envoyer un message privé à quelqu'un (il faut être connecté en privé avec cette personne au préalable)
	/file <pseudo> <fichier> : envoyer un fichier privé à quelqu'un (il faut être connecté en privé avec cette personne au préalable)
	/close <pseudo> : fermer la connexion privée avec quelqu'un (il faut être connecté en privé avec cette personne actuellement)
	/exit : quitter le chat (toutes les connexions actives seront fermées)
	
--------------------------------------------------------------------------------

Fichier de configuration

	Les fichiers de configurations sont chargés au lancement de l'application.
	Si aucun fichier de configuration ne peut être chargé, alors c'est la configuration par défaut du code source qui sera appliquée à l'exécution.
	
	Le caractère '#' est utilisé pour les commentaires dans le fichier de configuration. Tout ce qui suit ce caractère est alors ignoré lors du chargement du fichier.
	Le caractère '=' est utilisé comme opérateur d'affectation dans le fichier de configuration.
	
	Chaque champ doit être suivi du caractère d'affectation ainsi que d'un booléen "true" ou "false".

	Champ(s) disponible(s) dans les fichier de configuration du client et du serveur :
	COLORATOR
	HEADER
	ERROR
	WARNING
	INFO
	DEBUG
	
	Champ(s) disponible(s) dans les fichier de configuration du serveur uniquement :
	SELECT
	
--------------------------------------------------------------------------------

Comportement par défaut
	Si aucune option n'est fournie en ligne de commande : le logger utilise la sortie d'erreur standard (stderr) pour tous les évenements.
	Si aucun fichier de configuration n'est chargé : le logger n'affiche que les exceptions.
	
Valeurs du fichier de configuration par défaut du client : 
	COLORATOR=false
	HEADER=false
	ERROR=true
	WARNING=true
	INFO=false
	DEBUG=false
	
Valeurs du fichier de configuration par défaut du serveur : 
	COLORATOR=false
	HEADER=false
	ERROR=true
	WARNING=true
	INFO=false
	DEBUG=false
	SELECT=true
	
--------------------------------------------------------------------------------

Colorateur
	Le colorateur utilise les séquences d'échappement ANSI du terminal. 
	Il fonctionne correctement sur la plupart des terminaux GNU/Linux et améliore la lisibilité du logger.
	
Code couleur
	Rouge : erreur (problème critique)
	Jaune : avertissement (problème non critique)
	Vert : information sur les données échangées sur le réseau
	Violet : message de debug
	Bleu (serveur) : liste des clés dans le sélecteur
	Cyan (serveur) : liste des clés sélectionnées par le sélecteur

--------------------------------------------------------------------------------