package fr.upem.matou.tcp;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;

import fr.upem.matou.logger.Logger;

class SelectorDebug {
	static String interestOpsToString(SelectionKey key) {
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

	static String possibleActionsToString(SelectionKey key) {
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

	@SuppressWarnings("resource")
	static void printKeys(Selector selector) {
		Set<SelectionKey> keys = selector.keys();
		if (keys.isEmpty()) {
			Logger.selectInfo("The selector contains no key : this should not happen!");
			return;
		}
		Logger.selectInfo("The selector contains:");
		for (SelectionKey key : keys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				Logger.selectInfo("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				Logger.selectInfo("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	@SuppressWarnings("resource")
	static void printSelectedKeys(Set<SelectionKey> selectedKeys) {
		if (selectedKeys.isEmpty()) {
			Logger.selectInfo("There were not selected keys.");
			return;
		}
		Logger.selectInfo("The selected keys are :");
		for (SelectionKey key : selectedKeys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				Logger.selectInfo("\tServerSocketChannel can perform : " + possibleActionsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				Logger.selectInfo("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
			}

		}
	}

	static String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (@SuppressWarnings("unused") IOException ignored) {
			return "???";
		}
	}
}
