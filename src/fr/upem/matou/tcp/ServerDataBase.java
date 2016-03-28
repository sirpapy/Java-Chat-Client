package fr.upem.matou.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/*
 * This class represents the state of the chat server.
 */
class ServerDataBase {
	// TODO : Concurrence

	private final HashMap<SocketChannel, String> connected = new HashMap<>(); // XXX CURRENTLY NOT THREAD SAFE
	private final Set<SocketChannel> keysView = connected.keySet();
	private final Collection<String> valuesView = connected.values();
	private final ArrayDeque<ByteBuffer> broadcast = new ArrayDeque<>();

	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if (valuesView.contains(pseudo)) {
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

	Set<SocketChannel> connectedClients() {
		return Collections.unmodifiableSet(keysView);
	}

	void addBroadcast(ByteBuffer bbWriteAll) {
		broadcast.add(bbWriteAll);
	}

	void updateStateReadAll(Set<SelectionKey> keys) {
		ByteBuffer bbBroadcast = broadcast.pollFirst();
		System.out.println("BROADCAST : " + bbBroadcast);
		if (bbBroadcast == null) {
			return;
		}

		System.out.println("KEYS : " + keys);
		for (SelectionKey key : keys) {
			System.out.println("KEY : " + key);
			if (!key.isValid()) {
				System.out.println("\tINVALID");
				continue;
			}
			
			ServerSession session = (ServerSession) key.attachment();
			if (session == null) {
				System.out.println("\tNO SESSION");
				continue;
			}
			
			if(!session.isAuthent()) {
				System.out.println("\tNOT AUTHENT");
				continue;
			}
			
			System.out.println("\tOK");

			ByteBuffer bb = ByteBuffers.copy(bbBroadcast);
			session.appendWriteBuffer(bb);
			
			int ops = key.interestOps();
			key.interestOps(ops | SelectionKey.OP_WRITE);
		}
	}

}
