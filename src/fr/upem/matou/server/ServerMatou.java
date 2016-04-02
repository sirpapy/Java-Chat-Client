package fr.upem.matou.server;

import java.io.IOException;

import fr.upem.matou.server.network.ServerCore;

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

//		PrintStream out = new PrintStream(Files.newOutputStream(Paths.get("server_out.log"), WRITE, CREATE, APPEND), true);
//		PrintStream err = new PrintStream(Files.newOutputStream(Paths.get("server_err.log"), WRITE, CREATE, APPEND), true);
//		Logger.attachOutput(out);
//		Logger.attachError(err);

		int port = Integer.parseInt(args[0]);
		try (ServerCore server = new ServerCore(port)) {
			server.launch();
		}
	}
}
