package fr.upem.matou.logger;

import java.io.PrintStream;

public class Logger {

	public static enum LogType {
		READ, WRITE;
	}

	private static final PrintStream STREAM_OUTPUT = System.out;

	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_NETWORK = true;
	private static final boolean LOG_SELECT = true;
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

	public static void network(LogType type, String message) {
		if (LOG_NETWORK) {
			String string = "";
			switch (type) {
			case READ:
				string = "[R]";
				break;
			case WRITE:
				string = "[W]";
				break;
			default:
				break;
			}
			string = string + " " + message;
			STREAM_OUTPUT.println(string);
		}
	}

	public static void selectInfo(String message) {
		if (LOG_SELECT) {
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

	// TODO : Traçage des exceptions avec cette méthode
	public static void exception(Exception exception) {
		if (LOG_EXCEPTION) {
			exception.printStackTrace(STREAM_OUTPUT);
		}
	}
}
