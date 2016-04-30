package fr.upem.matou.server.network;

import static fr.upem.matou.shared.logger.Colorator.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;

/**
 * This class provides static methods in order to log selector events of the server.
 */
public class SelectorLogger {

	private static final PrintStream OUTPUT = System.out;

	private static boolean LOG_SELECT = false;

	private SelectorLogger() {
	}

	/**
	 * Enables or disables selector logging.
	 * 
	 * @param activation
	 *            true to enable or false to disable.
	 */
	public static void activateSelect(boolean activation) {
		LOG_SELECT = activation;
	}

	private static String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (@SuppressWarnings("unused") IOException ignored) {
			return "???";
		}
	}

	private static String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable()) {
			list.add("ACCEPT");
		}
		if (key.isReadable()) {
			list.add("READ");
		}
		if (key.isWritable()) {
			list.add("WRITE");
		}
		return String.join(" & ", list);
	}

	private static String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
			list.add("OP_ACCEPT");
		}
		if ((interestOps & SelectionKey.OP_READ) != 0) {
			list.add("OP_READ");
		}
		if ((interestOps & SelectionKey.OP_WRITE) != 0) {
			list.add("OP_WRITE");
		}
		return String.join(" | ", list);
	}

	private static void printLogSelector(String message) {
		if (LOG_SELECT) {
			OUTPUT.println(colorBlue(message));
		}
	}

	private static void printLogSelectedKeys(String message) {
		if (LOG_SELECT) {
			OUTPUT.println(colorCyan(message));
		}
	}

	@SuppressWarnings("resource")
	static void logSelector(Selector selector) {
		Set<SelectionKey> keys = selector.keys();
		if (keys.isEmpty()) {
			printLogSelector("The selector contains no key : this should not happen!");
			return;
		}
		printLogSelector("The selector contains:");
		for (SelectionKey key : keys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				printLogSelector("\tKey for Server : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				printLogSelector("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	@SuppressWarnings("resource")
	static void logSelectedKeys(Set<SelectionKey> selectedKeys) {
		if (selectedKeys.isEmpty()) {
			printLogSelectedKeys("There were no selected keys.");
			return;
		}
		printLogSelectedKeys("The selected keys are :");
		for (SelectionKey key : selectedKeys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				printLogSelectedKeys("\tServer can perform : " + possibleActionsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				printLogSelectedKeys(
						"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
			}

		}
	}

}
