package fr.upem.matou.logger;

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
}
