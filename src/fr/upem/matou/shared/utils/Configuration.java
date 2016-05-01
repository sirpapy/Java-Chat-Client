package fr.upem.matou.shared.utils;

import static java.util.Objects.requireNonNull;

/**
 * This class provides static methods in order to parse a configuration file.
 */
public class Configuration {
	private static final char COMMENT_SYMBOL = '#';
	private static final char AFFECTATION_SYMBOL = '=';

	private Configuration() {
	}

	/**
	 * An object that represents a configuration entry.
	 */
	public static class ConfigEntry {
		private final String command;
		private final String argument;

		ConfigEntry(String command, String argument) {
			this.command = command;
			this.argument = argument;
		}

		/**
		 * Returns the command of this entry.
		 * 
		 * @return The command.
		 */
		public String getCommand() {
			return command;
		}

		/**
		 * Returns the argument of this entry.
		 * 
		 * @return The argument.
		 */
		public String getArgument() {
			return argument;
		}
	}

	/**
	 * Returns a line without comments.
	 * 
	 * @param line
	 *            The raw line.
	 * @return The line without comments.
	 */
	public static String removeComments(String line) {
		requireNonNull(line);
		int comment = line.indexOf(COMMENT_SYMBOL);
		if (comment == -1) {
			return line;
		}
		return line.substring(0, comment);
	}

	/**
	 * Tests if the given line is a valid affectation.
	 * 
	 * @param line
	 *            The line
	 * @return true if this line is an affectation or false otherwise.
	 */
	public static boolean isAffectation(String line) {
		requireNonNull(line);
		return line.chars().anyMatch(c -> c == AFFECTATION_SYMBOL);
	}

	/**
	 * Parses a single configuration line to a ConfigEntry.
	 * 
	 * @param line
	 *            The line
	 * @return A ConfigEntry describing this line.
	 */
	public static ConfigEntry parseLine(String line) {
		requireNonNull(line);
		int limit = line.indexOf(AFFECTATION_SYMBOL);
		if (limit == -1) {
			throw new AssertionError("This line is not a valid configuration affectation");
		}
		String command = line.substring(0, limit);
		String argument = line.substring(limit + 1);
		return new ConfigEntry(command, argument);
	}
}
