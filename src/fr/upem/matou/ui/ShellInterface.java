package fr.upem.matou.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;

/**
 * This class provides a user interface from a shell.
 */
public class ShellInterface implements UserInterface {

	private final InputStream input = System.in;
	private final Scanner scanner = new Scanner(input);
	private final PrintStream output = System.out;

	private Optional<String> readLine() {
		if (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			return Optional.of(line);
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> readPseudo() {
		output.print("Pseudo > ");
		return readLine();
	}

	@Override
	public Optional<String> readMessage() {
		return readLine();
	}

	@Override
	public void displayMessage(Message message) {
		boolean isPrivate = message.isPrivate();
		String pseudo = message.getPseudo();
		String content = message.getContent();

		String string;
		if (isPrivate) {
			string = "#" + pseudo + "#";

		} else {
			string = "<" + pseudo + ">";
		}
		string = string + " " + content;

		output.println(string);
	}

	@Override
	public void displayNewConnectionEvent(String pseudo) {
		output.println("<" + pseudo + " joins the chat>");
	}

	@Override
	public void displayNewDisconnectionEvent(String pseudo) {
		output.println("<" + pseudo + " left the chat>");
	}

}
