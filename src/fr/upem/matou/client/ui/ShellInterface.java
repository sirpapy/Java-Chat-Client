package fr.upem.matou.client.ui;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.Message;
import fr.upem.matou.shared.network.ErrorType;
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

	private String readLine() throws IOException {
		if (!scanner.hasNextLine()) {
			throw new IOException("InputStream source of scanner is closed");
		}
		String line = scanner.nextLine();
		return line;
	}

	@Override
	public String getUsername() throws IOException {
		output.print("Username > ");
		return readLine();
	}

	@Override
	public Optional<ClientEvent> getEvent() throws IOException {
		while (true) {
			String line = readLine();
			if (ShellCommand.isExit(line)) {
				return Optional.empty();
			}
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
		Username username = message.getUsername();
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
	public void displayNewConnectionEvent(Username username) {
		requireNonNull(username);
		output.println("<" + username + " joins the chat>");
	}

	@Override
	public void displayNewDisconnectionEvent(Username username) {
		requireNonNull(username);
		output.println("<" + username + " left the chat>");
	}

	@Override
	public void displayNewPrivateRequestEvent(Username username) {
		requireNonNull(username);
		output.println("<" + username + " asks for a private connection>");
	}

	@Override
	public void displayNewPrivateAcceptionEvent(Username username) {
		requireNonNull(username);
		output.println("<" + username + " accepts the private connection>");
	}

	@Override
	public void displayNewFileReception(String username, Path path) {
		requireNonNull(username);
		requireNonNull(path);
		output.println("<" + username + " sends a file : " + path + ">");
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
	public void warnUnavailableUsername(String username) {
		error.println("This username is not available");
	}

	@Override
	public void warnInvalidEvent(ClientEvent event) {
		error.println("This message is not valid");
	}

	private void warnInvalidCommand() {
		error.println("Invalid command" + "\n<message> : send a public message"
				+ "\n/open <username> : ask for a private connection"
				+ "\n/accept <username> : accept a private connection"
				+ "\n/pv <username> <message> : send a private message"
				+ "\n/file <username> <filepath> : send a private file");
	}

	@Override
	public void displayError(ErrorType type) {
		requireNonNull(type);

		switch (type) {

		case USRNOTCO:
			error.println("This user is not connected");
			break;

		case USRNOTPVREQ:
			error.println("This user did not send a private connection request");
			break;

		case UNK:
		default:
			error.println("Unknown error");
			break;

		}
	}

	@Override
	public void close() throws IOException {
		scanner.close();
	}

}
