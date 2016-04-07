package fr.upem.matou.client.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.ClientEvent.*;

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
			String pseudo = tokens[1];
			String message = Arrays.stream(tokens).skip(2).collect(Collectors.joining(" "));
			return Optional.of(new ClientEventSendPrivateMessage(pseudo, message));
		}

		case "file": {
			String pseudo = tokens[1];
			Path path = Paths.get(tokens[2]);
			return Optional.of(new ClientEventSendPrivateFile(pseudo, path));
		}

		case "open": {
			String pseudo = tokens[1];
			return Optional.of(new ClientEventOpenPrivate(pseudo));
		}

		case "close": {
			String pseudo = tokens[1];
			return Optional.of(new ClientEventClosePrivate(pseudo));
		}

		case "accept": {
			String pseudo = tokens[1];
			return Optional.of(new ClientEventAcceptPrivate(pseudo));
		}

		default:
			return Optional.empty();

		}
	}
}
