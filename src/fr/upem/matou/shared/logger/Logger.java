package fr.upem.matou.shared.logger;

import static fr.upem.matou.shared.logger.Colorator.colorGreen;
import static fr.upem.matou.shared.logger.Colorator.colorPurple;
import static fr.upem.matou.shared.logger.Colorator.colorRed;
import static fr.upem.matou.shared.logger.Colorator.colorYellow;
import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;

/**
 * This class provides static methods in order to log events by priority.
 */
public class Logger {

	/**
	 * This enum describes the direction of a network event (incoming or outgoing).
	 */
	public static enum NetworkLogType {

		/**
		 * Incoming event.
		 */
		READ,

		/**
		 * Outgoing event.
		 */
		WRITE;

	}

	private static PrintStream OUTPUT = System.err;
	private static PrintStream EXCEPT = System.err;

	private static final boolean HEADER_INFO = true; // displays more info

	private static final boolean LOG_DEBUG = true;
	private static final boolean LOG_NETWORK = true;
	private static final boolean LOG_WARNING = true;
	private static final boolean LOG_ERROR = true;
	private static final boolean LOG_EXCEPTION = true;

	private static final String SEPARATOR = " | ";

	private Logger() {
	}

	/**
	 * Changes the normal output of the logger.
	 * 
	 * @param out
	 *            The new normal output.
	 */
	public static void attachOutput(PrintStream out) {
		requireNonNull(out);
		OUTPUT = out;
	}

	/**
	 * Changes the exception output of the logger.
	 * 
	 * @param except
	 *            The new exception output.
	 */
	public static void attachException(PrintStream except) {
		requireNonNull(except);
		EXCEPT = except;
	}

	private static String localAddressToString(SocketChannel sc) {
		try {
			return sc.getLocalAddress().toString();
		} catch (@SuppressWarnings("unused") Exception __) {
			return "???";
		}
	}

	private static String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (@SuppressWarnings("unused") Exception __) {
			return "???";
		}
	}

	private static String formatLog(String level, String message) {
		if (!HEADER_INFO) {
			return message;
		}
		long now = System.currentTimeMillis();
		String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(now);

		String thread = Thread.currentThread().getName();

		return String.join(SEPARATOR, level, time, thread, message);
	}

	/**
	 * Formats a message associated with a SocketChannel.
	 * 
	 * @param sc
	 *            The channel.
	 * @param message
	 *            The message.
	 * @return The formated message.
	 */
	public static String formatNetworkData(SocketChannel sc, String message) {
		String local = localAddressToString(sc);
		String remote = remoteAddressToString(sc);
		return String.join(SEPARATOR, local + " -> " + remote, message);
	}

	/**
	 * Formats a network request associated with a SocketChannel.
	 * 
	 * @param sc
	 *            The channel.
	 * @param type
	 *            The direction of the request.
	 * @param message
	 *            The message.
	 * @return The formated message.
	 */
	public static String formatNetworkRequest(SocketChannel sc, NetworkLogType type, String message) {
		String direction = "";
		switch (type) {
		case READ:
			direction = "READ";
			break;
		case WRITE:
			direction = "WRITE";
			break;
		default:
			break;
		}

		if (!HEADER_INFO) {
			return String.join(SEPARATOR, direction, message);
		}

		String local = localAddressToString(sc);
		String remote = remoteAddressToString(sc);

		return String.join(SEPARATOR, local + " -> " + remote, direction, message);
	}

	/**
	 * Logs a debug message to the error output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void debug(String message) {
		if (LOG_DEBUG) {
			OUTPUT.println(colorPurple(formatLog("DEBUG", message)));
		}
	}

	/**
	 * Logs a network event to the normal output.
	 * 
	 * @param message
	 *            The message
	 */
	public static void info(String message) {
		if (LOG_NETWORK) {
			OUTPUT.println(colorGreen(formatLog("INFO", message)));
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
			OUTPUT.println(colorYellow(formatLog("WARNING", message)));
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
			OUTPUT.println(colorRed(formatLog("ERROR", message)));
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
			EXCEPT.println("--------------------------------------------------------------------------------");
			exception.printStackTrace(EXCEPT);
			EXCEPT.println("--------------------------------------------------------------------------------");
		}
	}

}
