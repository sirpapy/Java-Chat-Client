package fr.upem.matou.ui;

import java.io.InputStream;
import java.util.Scanner;

public class ShellInterface implements UserInterface {

	private final InputStream input = System.in;
	private final Scanner scanner = new Scanner(input);
	
	private String readLine() {
		return scanner.nextLine();
	}
	
	@Override
	public String readPseudo() {
		System.out.print("Pseudo > ");
		return readLine();
	}

	@Override
	public String readMessage() {
		System.out.print("Message > ");
		return readLine();
	}

}
