package fr.upem.matou.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import fr.upem.matou.buffer.ByteBuffers;
import fr.upem.matou.logger.Logger;

/*
 * This class represents the state of the chat server.
 * This class is not thread-safe and should not be used by several threads.
 */
class ServerDataBase {

	private static final int PSEUDO_MAX_LENGTH = 32;
	private static final int MESSAGE_MAX_LENGTH = 32;

	private final HashMap<SocketChannel, String> connected = new HashMap<>();
	private final Set<SocketChannel> keysView = connected.keySet();
	private final Collection<String> valuesView = connected.values();
	private final ArrayDeque<ByteBuffer> broadcast = new ArrayDeque<>();
	private final Set<SelectionKey> keys;

	public ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;
	}

	private static boolean isValidPseudoCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint);
	}

	private static boolean isValidMessageCharacter(int codePoint) {
		return !Character.isISOControl(codePoint);
	}

	private static boolean checkPseudoValidity(String pseudo) {
		return pseudo.length() <= PSEUDO_MAX_LENGTH && pseudo.chars().allMatch(ServerDataBase::isValidPseudoCharacter);
	}

	static boolean checkMessageValidity(String message) {
		return message.length() <= MESSAGE_MAX_LENGTH
				&& message.chars().allMatch(ServerDataBase::isValidMessageCharacter);
	}

	private boolean checkAvailability(String pseudo) {
		return !valuesView.contains(pseudo);
	}

	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewClient(SocketChannel sc, String pseudo) {
		if (!(checkAvailability(pseudo) && checkPseudoValidity(pseudo))) {
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

	void addBroadcast(ByteBuffer bbWriteAll) {
		broadcast.add(bbWriteAll);
	}

	void updateStateReadAll() {
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

			if (!session.isAuthent()) {
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

	void updateStateWriteAll() {
		// TEMP
	}

	String removeClient(SocketChannel channel) {
		String disconnected = connected.remove(channel);
		if (disconnected == null) {
			Logger.debug("DISCONNECTION : {UNAUTHENTIFIED CLIENT}");
		} else {
			Logger.debug("DISCONNECTION : " + disconnected);
		}

		return disconnected;
	}

}
