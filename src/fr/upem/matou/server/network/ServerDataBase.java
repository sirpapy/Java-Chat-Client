package fr.upem.matou.server.network;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import fr.upem.matou.logger.Logger;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

/*
 * This class represents the state of the chat server.
 * This class is not thread-safe and should not be used by several threads.
 */
class ServerDataBase {
	private static final int BUFFER_SIZE_BROADCAST = NetworkProtocol.getMaxOutgoingRequestSize();

	private final HashMap<SocketChannel, String> connected = new HashMap<>();
	private final Set<SocketChannel> keysView = connected.keySet();
	private final Collection<String> valuesView = connected.values();
	private final Set<SelectionKey> keys;
	private final ByteBuffer bbBroadcast = ByteBuffer.allocateDirect(BUFFER_SIZE_BROADCAST);

	public ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;
	}

	private boolean checkAvailability(String pseudo) {
		return !valuesView.contains(pseudo);
	}

	ByteBuffer getBroadcastBuffer() {
		return bbBroadcast;
	}
	
	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if (!(checkAvailability(pseudo))) {
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

	void updateStateReadAll() {
		Logger.debug("BROADCAST : " + bbBroadcast);
		if(bbBroadcast.position() == 0) {
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

			if (!session.isAuthent()) {
				Logger.debug("\tNOT AUTHENT");
				continue;
			}

			Logger.debug("\tOK");

			session.appendWriteBuffer(bbBroadcast);

			int ops = key.interestOps();
			key.interestOps(ops | SelectionKey.OP_WRITE);
		}
		
		bbBroadcast.clear();
	}

	void updateStateWriteAll() {
		// TEMP
	}

	String removeClient(SocketChannel channel) {
		String disconnected = connected.remove(channel);
		if (disconnected == null) {
			Logger.debug("DISCONNECTION : {UNAUTHENTICATED CLIENT}");
		} else {
			Logger.debug("DISCONNECTION : " + disconnected);
		}

		return disconnected;
	}

}
