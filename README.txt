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
	-logger path : rediriger la sortie normale du logger dans un fichier (au lieu de la sortie d'erreur du terminal)
	-exception path : rediriger la sortie des exception du logger dans un fichier (au lieu de la sortie d'erreur du terminal)
	
--------------------------------------------------------------------------------

Fichier de configuration

	S'ils existent, les fichiers de configurations sont chargés au lancement de l'application. 
	
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