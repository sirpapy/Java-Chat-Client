package fr.upem.matou.client;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
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
		try (Stream<String> lines = Files.lines(CLIENT_CONFIG)) {
			lines.map(Configuration::removeComments).filter(Configuration::isAffectation)
					.forEach(ClientMatou::loadConfigLine);
		} catch (@SuppressWarnings("unused") IOException __) {
			return;
		}
	}
	
	private static void usage() {
		System.err.println("Usage : host port [username]");
	}

	/**
	 * Main method of the chat client program.
	 * 
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			usage();
			return;
		}

		loadConfig();
		
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		String username = null;
		if(args.length >= 3) {
			username = args[2];
		}
		
		try (ClientCore client = new ClientCore(host, port)) {
			if(username==null) {
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
