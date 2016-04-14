package fr.upem.matou.client.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.ClientEvent.*;

/*
 * This class is used to convert an input string command to a ClientEvent.
 */
class ShellCommand {

	private static final String COMMAND_TOKEN = "/";

	private ShellCommand() {
	}

	private static boolean isCommandMessage(String input) {
		return input.startsWith(COMMAND_TOKEN);
	}

	static Optional<ClientEvent> parseLine(String input) {
		if (!isCommandMessage(input)) {
			return Optional.of(new ClientEventSendMessage(input));
		}
		String[] tokens = input.split(" ");

		String command = tokens[0].replace(COMMAND_TOKEN, "");

		switch (command) {

		case "pv": {
			if (tokens.length < 3) {
				return Optional.empty();
			}
			String username = tokens[1];
			String message = Arrays.stream(tokens).skip(2).collect(Collectors.joining(" "));
			return Optional.of(new ClientEventSendPrivateMessage(username, message));
		}

		case "file": {
			if (tokens.length != 3) {
				return Optional.empty();
			}
			String username = tokens[1];
			Path path = Paths.get(tokens[2]);
			return Optional.of(new ClientEventSendPrivateFile(username, path));
		}

		case "open": {
			if (tokens.length != 2) {
				return Optional.empty();
			}
			String username = tokens[1];
			return Optional.of(new ClientEventOpenPrivate(username));
		}

		case "close": {
			if (tokens.length != 2) {
				return Optional.empty();
			}
			String username = tokens[1];
			return Optional.of(new ClientEventClosePrivate(username));
		}

		case "accept": {
			if (tokens.length != 2) {
				return Optional.empty();
			}
			String username = tokens[1];
			return Optional.of(new ClientEventAcceptPrivate(username));
		}

		default:
			// TODO : Commande "Help" pour afficher la liste des commandes disponibles
			return Optional.empty();

		}
	}
}
