package fr.upem.matou.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

import fr.upem.matou.client.network.ClientCore;
import fr.upem.matou.shared.logger.Colorator;
import fr.upem.matou.shared.logger.Logger;

/**
 * Main class of the client Matou.
 */
public class ClientMatou {

	private ClientMatou() {
	}

	private static void usage() {
		System.err.println("Usage : [options] host port [username]" + "\nAvailable options :"
				+ "\n-error : enables error logging" + "\n-warning : enables warning logging"
				+ "\n-info : enables info logging" + "\n-debug : enables debug logging"
				+ "\n-header : enables header logs displaying" + "\n-noexception : disables exception logging"
				+ "\n-color : enables colorated logger"
				+ "\n-logoutput path : redirects the normal output of the logger to the given path"
				+ "\n-logexcept path : redirects the exception output of the logger to the given path");
	}

	/**
	 * Main method of the chat client program.
	 * 
	 * @param args
	 *            Command line arguments.
	 * @throws FileNotFoundException
	 *             If some other error occurs while opening or creating the log file.
	 */
	public static void main(String[] args) throws FileNotFoundException {

		int opt;
		for (opt = 0; opt < args.length; opt++) {

			if (!args[opt].startsWith("-")) {
				break;
			}

			switch (args[opt]) {

			case "-error": {
				Logger.activateError(true);
				break;
			}

			case "-warning": {
				Logger.activateWarning(true);
				break;
			}

			case "-info": {
				Logger.activateInfo(true);
				break;
			}

			case "-debug": {
				Logger.activateDebug(true);
				break;
			}

			case "-header": {
				Logger.activateHeader(true);
				break;
			}

			case "-noexception": {
				Logger.activateException(false);
				break;
			}

			case "-color": {
				Colorator.activateColorator(true);
				break;
			}

			case "-logoutput": {
				String arg = args[++opt];
				PrintStream ps = new PrintStream(arg);
				Logger.attachOutput(ps);
				break;
			}

			case "-logexcept": {
				String arg = args[++opt];
				PrintStream ps = new PrintStream(arg);
				Logger.attachException(ps);
				break;
			}

			case "-help": {
				usage();
				return;
			}

			default:
				System.err.println("Unknown option : " + args[opt]);
				usage();
				return;

			}
		}

		String host;
		int port;
		String username = null;
		try {
			host = args[opt];
			port = Integer.parseInt(args[opt + 1]);

			if (opt + 2 == args.length - 1) {
				username = args[opt + 2];
			}
		} catch (@SuppressWarnings("unused") IndexOutOfBoundsException __) {
			usage();
			return;
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
