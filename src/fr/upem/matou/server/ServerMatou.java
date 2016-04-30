package fr.upem.matou.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import fr.upem.matou.server.network.ServerCore;
import fr.upem.matou.server.network.SelectorLogger;
import fr.upem.matou.shared.logger.Colorator;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.utils.Configuration;
import fr.upem.matou.shared.utils.Configuration.ConfigEntry;

/**
 * Main class of the server Matou.
 */
public class ServerMatou {

	private static final Path SERVER_CONFIG = Paths.get("./config/server.conf");

	private ServerMatou() {
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
		case "SELECT": {
			boolean activation = Boolean.parseBoolean(argument);
			SelectorLogger.activateSelect(activation);
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

	private static void loadConfig() {
		try (Stream<String> lines = Files.lines(SERVER_CONFIG)) {
			lines.map(Configuration::removeComments).filter(Configuration::isAffectation)
					.forEach(ServerMatou::loadConfigLine);
		} catch (@SuppressWarnings("unused") IOException __) {
			return;
		}
	}

	private static void usage() {
		System.err.println("Usage : [options] port" + "\nAvailable options :" + "\n-help : displays all options"
				+ "\n-logger path : redirects the normal output of the logger to the given path"
				+ "\n-exception path : redirects the exception output of the logger to the given path");
	}

	/**
	 * Main method of the chat server program.
	 * 
	 * @param args
	 *            Command line arguments
	 * @throws FileNotFoundException
	 *             If some other error occurs while opening or creating the log file.
	 * 
	 */
	public static void main(String[] args) throws FileNotFoundException {

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
		if (remaining != 1) { // Incorrect number of remaining arguments
			usage();
			return;
		}

		loadConfig();

		int port = Integer.parseInt(args[opt]);

		try (ServerCore server = new ServerCore(port)) {
			server.launch();
		} catch (IOException e) {
			Logger.error(e.toString());
			Logger.exception(e);
		}
	}
}
