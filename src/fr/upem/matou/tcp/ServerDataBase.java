package fr.upem.matou.tcp;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;

/*
 * This class represents the state of the chat server.
 */
class ServerDataBase {
	// TODO : Concurrence

	private final HashMap<SocketChannel, String> connected = new HashMap<>(); // XXX CURRENTLY NOT THEAD SAFE
	private final Collection<String> pseudoView = connected.values();

	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if (pseudoView.contains(pseudo)) {
			return false;
		}
		connected.put(sc, pseudo);
		return true;
	}

	/*
	 * Returns the pseudo associated with this SocketChannel.
	 */
	String pseudoOf(SocketChannel sc) {
		return connected.get(sc);
	}
}
