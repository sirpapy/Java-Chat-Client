package fr.upem.matou.server;

import java.io.IOException;

import fr.upem.matou.server.network.ServerCore;
import fr.upem.matou.shared.logger.Logger;

/**
 * Main class of the server Matou.
 */
public class ServerMatou {

	private ServerMatou() {
	}

	private static void usage() {
		System.err.println("Usage : port");
	}

	/**
	 * Main method of the chat server program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			return;
		}

		int port = Integer.parseInt(args[0]);
		try (ServerCore server = new ServerCore(port)) {
			server.launch();
		} catch (IOException e) {
			Logger.error(e.toString());
			Logger.exception(e);
		}
	}
}
