package fr.upem.matou.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * This class provides a user interface from a shell.
 */
public class ShellInterface implements UserInterface {

	private final InputStream input = System.in;
	private final Scanner scanner = new Scanner(input);
	private final PrintStream output = System.out;

	private String readLine() {
		return scanner.nextLine();
	}

	@Override
	public String readPseudo() {
		output.print("Pseudo > ");
		return readLine();
	}

	@Override
	public String readMessage() {
		output.print("Message > ");
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

}
