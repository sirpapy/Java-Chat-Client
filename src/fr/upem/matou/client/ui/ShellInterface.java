package fr.upem.matou.client.ui;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.Message;
import fr.upem.matou.shared.network.Username;

/**
 * This class provides a user interface from a shell.
 */
public class ShellInterface implements UserInterface {
	private static final String INPUT_CHARSET = "UTF-8";

	private final InputStream input = System.in;
	private final PrintStream output = System.out;
	private final PrintStream error = System.err;
	private final Scanner scanner = new Scanner(input, INPUT_CHARSET);

	private Optional<String> readLine() {
		if (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			return Optional.of(line);
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> getUsername() {
		output.print("Username > ");
		return readLine();
	}

	@Override
	public Optional<ClientEvent> getEvent() {
		while (true) {
			Optional<String> optional = readLine();
			if (!optional.isPresent()) {
				return Optional.empty();
			}

			String line = optional.get();
			Optional<ClientEvent> event = ShellCommand.parseLine(line);
			if (!event.isPresent()) {
				warnInvalidCommand();
				continue;
			}
			return event;
		}
	}

	@Override
	public void displayMessage(Message message) {
		requireNonNull(message);
		
		boolean isPrivate = message.isPrivate();
		String username = message.getUsername();
		String content = message.getContent();

		String string;
		if (isPrivate) {
			string = "{" + username + "}";

		} else {
			string = "<" + username + ">";
		}
		string = string + " " + content;

		output.println(string);
	}

	@Override
	public void displayNewConnectionEvent(String username) {
		requireNonNull(username);
		output.println("<" + username + " joins the chat>");
	}

	@Override
	public void displayNewDisconnectionEvent(String username) {
		requireNonNull(username);
		output.println("<" + username + " left the chat>");
	}

	@Override
	public void displayNewPrivateRequestEvent(String username) {
		requireNonNull(username);
		output.println("<" + username + " asks for a private connection>");
	}

	@Override
	public void displayNewPrivateAcceptionEvent(String username) {
		requireNonNull(username);
		output.println("<" + username + " accepts the private connection>");
	}
	
	@Override
	public void displayNewFileReception(String username, Path path) {
		requireNonNull(username);
		requireNonNull(path);
		output.println("<" + username + " sends a file : " + path + ">");
	}	

	private void displayNewPrivateDisconnection(Username username) {
		// FIXME : Affiché en double (1 par thread)
	}

	@Override
	public void displayNewPrivateMessageDisconnection(Username username) {
		requireNonNull(username);
		output.println("<" + username + " : private messaging connection closed>"); 
	}

	@Override
	public void displayNewPrivateFileDisconnection(Username username) {
		requireNonNull(username);
		output.println("<" + username + " : private file exchanging connection closed>"); 
	}
	
	@Override
	public void warnInvalidUsername(String username) {
		error.println("This username is not valid");
	}

	@Override
	public void warnInvalidMessage(ClientEvent event) {
		error.println("This message is not valid");
	}

	private void warnInvalidCommand() {
		error.println("Invalid command");
	}

}
