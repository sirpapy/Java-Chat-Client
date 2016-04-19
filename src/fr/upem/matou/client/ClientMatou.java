package fr.upem.matou.client;

import java.io.IOException;
import java.util.Optional;

import fr.upem.matou.client.network.ClientCore;
import fr.upem.matou.shared.logger.Logger;

/**
 * Main class of the client Matou.
 */
public class ClientMatou {

	private ClientMatou() {
	}

	private static void usage() {
		System.err.println("Usage : host port [username]");
	}

	/**
	 * Main method of the client program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			usage();
			return;
		}

		// PrintStream out = new PrintStream(Files.newOutputStream(Paths.get("client_out.log"), WRITE, CREATE, APPEND),
		// true);
		// PrintStream err = new PrintStream(Files.newOutputStream(Paths.get("client_err.log"), WRITE, CREATE, APPEND),
		// true);
		// Logger.attachOutput(out);
		// Logger.attachError(err);

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		String username = null;
		if(args.length >= 3) {
			username = args[2];
		}
		
		try (ClientCore client = new ClientCore(host, port)) {
			client.startChat(Optional.ofNullable(username));
			/*
			 * TODO : Connexion en ligne de commande client.startChat(pseudo);
			 */
		} catch (IOException e) {
			Logger.error("CRITICAL ERROR | " + e.toString());
			Logger.exception(e);
		} catch (InterruptedException e) {
			Logger.warning("INTERRUPTION | " + e.toString());
		}
	}
}
