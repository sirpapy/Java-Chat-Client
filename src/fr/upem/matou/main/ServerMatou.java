package fr.upem.matou.main;

import java.io.IOException;

import fr.upem.matou.tcp.ServerCore;

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
	 * Main method of the server program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		int port = Integer.parseInt(args[0]);
		try (ServerCore server = new ServerCore(port)) {
			server.launch();
		}
	}
}
