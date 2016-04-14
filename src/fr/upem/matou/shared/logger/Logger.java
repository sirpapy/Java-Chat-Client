package fr.upem.matou.shared.logger;

import static fr.upem.matou.shared.logger.Colorator.*;
import static java.util.Objects.requireNonNull;

import java.io.PrintStream;

/**
 * This class provides static methods in order to log events by priority.
 */
public class Logger {

	/**
	 * This enum describes the direction of a network event (incoming or outgoing).
	 */
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

	// TODO : Prendre un objet à logger en paramètre afin d'afficher ses informations

	/**
	 * Changes the normal output of the logger.
	 * 
	 * @param out
	 *            The new normal output.
	 */
	public static void attachOutput(PrintStream out) {
		requireNonNull(out);
		STREAM_OUT = out;
	}

	/**
	 * Changes the error output of the logger.
	 * 
	 * @param err
	 *            The new error output.
	 */
	public static void attachError(PrintStream err) {
		requireNonNull(err);
		STREAM_ERR = err;
	}

	/**
	 * Logs a debug message to the error output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void debug(String message) {
		if (LOG_DEBUG) {
			STREAM_ERR.println(colorPurple(message));
		}
	}

	/**
	 * Logs a network event to the normal output.
	 * 
	 * @param type
	 *            The type of event
	 * @param message
	 *            The message
	 */
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

	/**
	 * Logs info of selection keys to the normal output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void selectInfo(String message) {
		if (LOG_SELECT) {
			STREAM_OUT.println(colorBlue(message));
		}
	}

	/**
	 * Logs info of selected keys to the normal output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void selectReadyInfo(String message) {
		if (LOG_SELECT) {
			STREAM_OUT.println(colorCyan(message));
		}
	}

	/**
	 * Logs a warning to the error output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void warning(String message) {
		if (LOG_WARNING) {
			STREAM_ERR.println(colorYellow(message));
		}
	}

	/**
	 * Logs an error to the error output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void error(String message) {
		if (LOG_ERROR) {
			STREAM_ERR.println(colorRed(message));
		}
	}

	/**
	 * Logs an exception to the error output.
	 * 
	 * @param exception
	 *            The message
	 */
	public static void exception(Exception exception) {
		if (LOG_EXCEPTION) {
			exception.printStackTrace(STREAM_ERR);
		}
	}
}
