package fr.upem.matou.client;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import fr.upem.matou.client.network.ClientCore;
import fr.upem.matou.shared.logger.Colorator;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.utils.Configuration;
import fr.upem.matou.shared.utils.Configuration.ConfigEntry;

/**
 * Main class of the client Matou.
 */
public class ClientMatou {

	private static final Path CLIENT_CONFIG = Paths.get("./config/client.conf");

	private ClientMatou() {
	}

	private static void loadConfigLine(String line) {
		ConfigEntry entry = Configuration.parseLine(line);
		String command = entry.getCommand();
		String argument = entry.getArgument();
		System.out.println(command + ":" + argument);

		switch (command) {
		case "COLORATOR": {
			boolean activation = Boolean.parseBoolean(argument);
			Colorator.activateColorator(activation);
			break;
		}
		case "ERROR": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateError(activation);
			break;
		}
		case "WARNING": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateWarning(activation);
			break;
		}
		case "INFO": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateInfo(activation);
			break;
		}
		case "DEBUG": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateDebug(activation);
			break;
		}
		case "HEADER": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateHeader(activation);
			break;
		}
		default:
			break;
		}
	}

	private static void loadConfig() throws IOException {
		try (Stream<String> lines = Files.lines(CLIENT_CONFIG)) {
			lines.map(Configuration::removeComments).filter(Configuration::isAffectation)
					.forEach(ClientMatou::loadConfigLine);
		} catch (@SuppressWarnings("unused") NoSuchFileException __) {
			// There is no configuration file to load
			return;
		}
	}

	private static void usage() {
		System.err.println(
				"Usage : [options] host port [username]" + "\nAvailable options :" + "\n-help : displays all options"
						+ "\n-logger path : redirects the normal output of the logger to the given path"
						+ "\n-exception path : redirects the exception output of the logger to the given path");
	}

	/**
	 * Main method of the chat client program.
	 * 
	 * @param args
	 *            Command line arguments
	 * @throws IOException
	 *             If an I/O error occurs during initialization (loading the configuration file and parsing the command
	 *             line arguments)
	 * 
	 */
	public static void main(String[] args) throws IOException {

		int opt;
		for (opt = 0; opt < args.length; opt++) {

			if (!args[opt].startsWith("-")) {
				break;
			}

			switch (args[opt]) {

			case "-logger": {
				String arg = args[++opt];
				PrintStream ps = new PrintStream(arg);
				Logger.attachOutput(ps);
				break;
			}

			case "-exception": {
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
		// Now "opt" is the first index of non optional arguments

		int remaining = args.length - opt;
		if (remaining < 2 || remaining > 3) { 
			// Incorrect number of remaining arguments
			usage();
			return;
		}

		loadConfig();

		String host = args[opt];
		int port = Integer.parseInt(args[opt + 1]);

		String username = null; // Optional argument
		if (remaining == 3) {
			username = args[opt + 2];
		}

		try (ClientCore client = new ClientCore(host, port)) {
			if (username == null) {
				client.startChat();
			} else {
				client.startChat(username);
			}
		} catch (IOException e) {
			Logger.error(e.toString());
			Logger.exception(e);
		} catch (InterruptedException e) {
			Logger.warning(e.toString());
		}

	}
}
