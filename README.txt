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