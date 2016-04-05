package fr.upem.matou.debug.tcp;

import org.junit.Test;

import fr.upem.matou.client.ClientMatou;
import fr.upem.matou.server.ServerMatou;

@SuppressWarnings("static-method")
public class NetworkTest {

	private static final Runnable serverMain = () -> {
		try {
			String[] args = new String[1];
			args[0] = "7777";
			ServerMatou.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	private static final Runnable clientMain = () -> {
		try {
			String[] args = new String[2];
			args[0] = "localhost";
			args[1] = "7777";
			ClientMatou.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	@Test
	public void testConnection() throws InterruptedException {
		Thread server = new Thread(serverMain);
		Thread client = new Thread(clientMain);
		server.start();
		client.start();

		/*
		 * TODO Pape :
		 * 
		 * Trouver un moyen de rediriger le flux d'entrée/sortie standard du thread "client" vers des fichiers de test
		 * OU de lire/écrire des string dynamiquement.
		 * 
		 * Le but est de pouvoir écrire du code comme ça dans le client :
		 * 
		 * ClientInputRedirection.input("foo"); // Ecrit "foo" sur l'entrée standard de ShellInterface
		 * String output = ClientInputRedirection.output(); // Lit l'entrée standard de ShellInterface
		 * 
		 * OU BIEN
		 * 
		 * Path inputPath, outputPath; // On défini 2 fichiers de test (un en lecture, un en écriture)
		 * ClientInputRedirection.setInput(inputPath); // On redirige l'entrée standard de ShellInterface
		 * ClientInputRedirection.setOutput(outputPath); // On redirige la sortie standard de ShellInterface
		 * 
		 */

		client.join();
		server.interrupt();
	}

}
