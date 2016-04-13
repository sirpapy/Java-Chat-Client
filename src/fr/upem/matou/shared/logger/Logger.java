package fr.upem.matou.shared.logger;

import static fr.upem.matou.shared.logger.Colorator.*;
import static java.util.Objects.requireNonNull;

import java.io.PrintStream;

public class Logger {

	public static enum NetworkLogType {
		READ, WRITE;
	}

	private static PrintStream STREAM_OUT = System.out;
	private static PrintStream STREAM_ERR = System.err;

	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_NETWORK = true;
	private static final boolean LOG_SELECT = true;
	private static final boolean LOG_WARNING = true;
	private static final boolean LOG_ERROR = true;
	private static final boolean LOG_EXCEPTION = true;

	private Logger() {
	}

	/*
	 * TODO : Format
	 * 
	 * Prendre l'objet à logger en paramètre afin d'afficher ses informations :
	 * 
	 * Object.toString() + " | " + message
	 * ou
	 * Loggable.toLogString() + " | " + message
	 */

	public static void attachOutput(PrintStream out) {
		requireNonNull(out);
		STREAM_OUT = out;
	}

	public static void attachError(PrintStream err) {
		requireNonNull(err);
		STREAM_ERR = err;
	}

	public static void debug(String message) {
		if (LOG_DEBUG) {
			STREAM_ERR.println(colorPurple(message));
		}
	}

	public static void network(NetworkLogType type, String message) {
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
			STREAM_OUT.println(colorGreen(string));
		}
	}

	public static void selectInfo(String message) {
		if (LOG_SELECT) {
			STREAM_OUT.println(colorBlue(message));
		}
	}

	public static void selectReadyInfo(String message) {
		if (LOG_SELECT) {
			STREAM_OUT.println(colorCyan(message));
		}
	}

	public static void warning(String message) {
		if (LOG_WARNING) {
			STREAM_ERR.println(colorYellow(message));
		}
	}

	public static void error(String message) {
		if (LOG_ERROR) {
			STREAM_ERR.println(colorRed(message));
		}
	}

	public static void exception(Exception exception) {
		if (LOG_EXCEPTION) {
			exception.printStackTrace(STREAM_ERR);
		}
	}
}
