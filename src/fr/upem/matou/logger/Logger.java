package fr.upem.matou.logger;

import java.io.PrintStream;

public class Logger {
	private static final PrintStream STREAM_OUTPUT = System.out;

	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_INFO = true;
	private static final boolean LOG_WARNING = true;
	private static final boolean LOG_ERROR = true;
	private static final boolean LOG_EXCEPTION = true;

	private Logger() {
	}

	public static void debug(String message) {
		if (LOG_DEBUG) {
			STREAM_OUTPUT.println(message);
		}
	}

	public static void info(String message) {
		if (LOG_INFO) {
			STREAM_OUTPUT.println(message);
		}
	}

	public static void warning(String message) {
		if (LOG_WARNING) {
			STREAM_OUTPUT.println(message);
		}
	}
	
	public static void error(String message) {
		if (LOG_ERROR) {
			STREAM_OUTPUT.println(message);
		}
	}

	public static void exception(Exception exception) {
		if (LOG_EXCEPTION) {
			exception.printStackTrace(STREAM_OUTPUT);
		}
	}
}
