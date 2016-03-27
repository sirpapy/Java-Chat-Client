package fr.upem.matou.logger;

import java.io.PrintStream;

public class Logger {
	private static final PrintStream OUTPUT = System.out;

	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_INFO = true;
	private static final boolean LOG_WARNING = true;
	private static final boolean LOG_EXCEPTION = false;

	private Logger() {
	}

	public static void debug(String message) {
		if (LOG_DEBUG) {
			OUTPUT.println(message);
		}
	}

	public static void info(String message) {
		if (LOG_INFO) {
			OUTPUT.println(message);
		}
	}

	public static void warning(String message) {
		if (LOG_WARNING) {
			OUTPUT.println(message);
		}
	}

	public static void exception(Exception exception) {
		if (LOG_EXCEPTION) {
			OUTPUT.println(exception);
		}
	}
}
