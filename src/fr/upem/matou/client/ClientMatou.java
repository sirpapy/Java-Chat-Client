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
	 * Main method of the chat client program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			usage();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		String username = null;
		if(args.length >= 3) {
			username = args[2];
		}
		
		try (ClientCore client = new ClientCore(host, port)) {
			client.startChat(Optional.ofNullable(username));
		} catch (IOException e) {
			Logger.error(e.toString());
			Logger.exception(e);
		} catch (InterruptedException e) {
			Logger.warning(e.toString());
		}
	}
}
