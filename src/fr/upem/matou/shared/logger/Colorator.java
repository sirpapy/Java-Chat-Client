package fr.upem.matou.shared.logger;

/**
 * This class provides static methods to display colored string in a shell using the ANSI escape codes.
 * 
 * Inspired from :
 * http://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
 */
public class Colorator {
	private static boolean activated = true;

	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_BLACK = "\u001B[30m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_YELLOW = "\u001B[33m";
	private static final String ANSI_BLUE = "\u001B[34m";
	private static final String ANSI_PURPLE = "\u001B[35m";
	private static final String ANSI_CYAN = "\u001B[36m";
	private static final String ANSI_WHITE = "\u001B[37m";

	public static String colorBlack(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_BLACK + string + ANSI_RESET;
	}

	public static String colorRed(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_RED + string + ANSI_RESET;
	}

	public static String colorGreen(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_GREEN + string + ANSI_RESET;
	}

	public static String colorYellow(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_YELLOW + string + ANSI_RESET;
	}

	public static String colorBlue(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_BLUE + string + ANSI_RESET;
	}

	public static String colorPurple(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_PURPLE + string + ANSI_RESET;
	}

	public static String colorCyan(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_CYAN + string + ANSI_RESET;
	}

	public static String colorWhite(String string) {
		if (!activated) {
			return string;
		}
		return ANSI_WHITE + string + ANSI_RESET;
	}

	public static void printColors() {
		System.out.println(Colorator.colorBlack("BLACK"));
		System.out.println(Colorator.colorBlue("BLUE"));
		System.out.println(Colorator.colorCyan("CYAN"));
		System.out.println(Colorator.colorGreen("GREEN"));
		System.out.println(Colorator.colorPurple("PURPLE"));
		System.out.println(Colorator.colorRed("RED"));
		System.out.println(Colorator.colorWhite("WHITE"));
		System.out.println(Colorator.colorYellow("YELLOW"));
	}

	public static void main(String[] args) {
		printColors();
	}
}