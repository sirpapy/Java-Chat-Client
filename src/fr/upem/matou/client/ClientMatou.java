package fr.upem.matou.client;

// TODO : Package "server"

import java.io.IOException;

import fr.upem.matou.client.network.ClientCore;

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

		try (ClientCore client = new ClientCore(host, port)) {
			client.startChat();
		}
	}
}