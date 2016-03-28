package fr.upem.matou.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import fr.upem.matou.logger.Logger;

/*
 * This class represents the state of the chat server.
 */
class ServerDataBase {
	// TODO : Concurrence

	private final HashMap<SocketChannel, String> connected = new HashMap<>(); // XXX CURRENTLY NOT THREAD SAFE
	private final Set<SocketChannel> keysView = connected.keySet();
	private final Collection<String> valuesView = connected.values();
	private final ArrayDeque<ByteBuffer> broadcast = new ArrayDeque<>();

	
	private static boolean checkValidity(String pseudo) {
		return pseudo.chars().allMatch(Character::isLetterOrDigit);
	}
	
	private boolean checkAvailability(String pseudo) {
		return !valuesView.contains(pseudo);
	}
	
	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if(!(checkAvailability(pseudo) && checkValidity(pseudo))) {
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
		Logger.debug("BROADCAST : " + bbBroadcast);
		if (bbBroadcast == null) {
			return;
		}

		Logger.debug("KEYS NUMBER : " + keys.size());
		for (SelectionKey key : keys) {
			Logger.debug("KEY : " + key);
			if (!key.isValid()) {
				Logger.debug("\tINVALID");
				continue;
			}
			
			ServerSession session = (ServerSession) key.attachment();
			if (session == null) {
				Logger.debug("\tNO SESSION");
				continue;
			}
			
			if(!session.isAuthent()) {
				Logger.debug("\tNOT AUTHENT");
				continue;
			}
			
			Logger.debug("\tOK");

			ByteBuffer bb = ByteBuffers.copy(bbBroadcast);
			session.appendWriteBuffer(bb);
			
			int ops = key.interestOps();
			key.interestOps(ops | SelectionKey.OP_WRITE);
		}
	}

}
