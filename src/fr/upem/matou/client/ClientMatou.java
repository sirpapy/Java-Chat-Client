package fr.upem.matou.client;

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
		
//		PrintStream out = new PrintStream(Files.newOutputStream(Paths.get("client_out.log"), WRITE, CREATE, APPEND), true);
//		PrintStream err = new PrintStream(Files.newOutputStream(Paths.get("client_err.log"), WRITE, CREATE, APPEND), true);
//		Logger.attachOutput(out);
//		Logger.attachError(err);
		
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		try (ClientCore client = new ClientCore(host, port)) {
			client.startChat();
		}
	}
}
