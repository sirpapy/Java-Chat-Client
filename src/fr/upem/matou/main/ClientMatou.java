package fr.upem.matou.main;

import java.io.IOException;

import fr.upem.matou.tcp.ClientCoreClean;

/**
 * Main class of the client Matou.
 */
public class ClientMatou {

	private ClientMatou() {
	}

	private static void usage() {
		System.err.println("Usage : host port");
	}

	/**
	 * Main method of the client program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 2) {
			usage();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);

		try (ClientCoreClean client = new ClientCoreClean(host, port)) {
			client.startChat();
		}
	}
}
