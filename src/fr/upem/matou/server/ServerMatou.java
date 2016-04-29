package fr.upem.matou.server;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import fr.upem.matou.server.network.ServerCore;
import fr.upem.matou.server.network.ServerLogger;
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

	@SuppressWarnings("resource")
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
			ServerLogger.activateSelect(activation);
			break;
		}
		case "HEADER": {
			boolean activation = Boolean.parseBoolean(argument);
			Logger.activateHeader(activation);
			break;
		}
		case "LOG_OUTPUT": {
			try {
				PrintStream ps = new PrintStream(argument);
				Logger.attachOutput(ps);
			} catch (@SuppressWarnings("unused") IOException __) {
				// Ignored
			}
			break;
		}
		case "LOG_EXCEPT": {
			try {
				PrintStream ps = new PrintStream(argument);
				Logger.attachException(ps);
			} catch (@SuppressWarnings("unused") IOException __) {
				// Ignored
			}
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

		loadConfig();

		int port = Integer.parseInt(args[0]);
		try (ServerCore server = new ServerCore(port)) {
			server.launch();
		} catch (IOException e) {
			Logger.error(e.toString());
			Logger.exception(e);
		}
	}
}
