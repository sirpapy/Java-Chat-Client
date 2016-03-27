package fr.upem.matou.nonblocking.test;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;

// TODO : Concurrence 
class ServerDataBase {
	private final HashMap<SocketChannel, String> connected = new HashMap<>();
	private final Collection<String> pseudoView = connected.values();
	
	/*
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if(pseudoView.contains(pseudo)) {
			return false;
		}
		connected.put(sc, pseudo);
		return true;
	}

	public String pseudoOf(SocketChannel sc) {
		return connected.get(sc);
	}
}
