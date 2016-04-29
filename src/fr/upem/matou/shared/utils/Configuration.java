package fr.upem.matou.shared.utils;

public class Configuration {
	private static final String COMMENT_SYMBOL = "#";
	private static final String AFFECTATION_SYMBOL = "=";

	public static class ConfigEntry {
		private final String command;
		private final String argument;

		public ConfigEntry(String command, String argument) {
			this.command = command;
			this.argument = argument;
		}

		public String getCommand() {
			return command;
		}

		public String getArgument() {
			return argument;
		}
	}

	public static String removeComments(String line) {
		int comment = line.indexOf('#');
		if (comment == -1) {
			return line;
		}
		return line.substring(0, comment);
	}

	public static boolean isAffectation(String line) {
		return line.contains(AFFECTATION_SYMBOL);
	}

	public static ConfigEntry parseLine(String line) {
		if (!isAffectation(line)) {
			throw new AssertionError("This line is not an valid configuration affectation");
		}
		int limit = line.indexOf(AFFECTATION_SYMBOL);
		String command = line.substring(0, limit);
		String argument = line.substring(limit + 1);
		return new ConfigEntry(command, argument);
	}
}
