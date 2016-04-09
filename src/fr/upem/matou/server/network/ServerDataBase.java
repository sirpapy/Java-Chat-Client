package fr.upem.matou.server.network;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class represents the state of the chat server.
 * This class is not thread-safe and should not be used by several threads.
 */
@SuppressWarnings("resource")
class ServerDataBase {
	private static final int BUFFER_SIZE_BROADCAST = NetworkProtocol.getMaxServerToClientRequestSize();

	private final HashMap<SocketChannel, ServerSession> sessions = new HashMap<>();
	private final HashMap<SocketChannel, Username> connected = new HashMap<>();
	private final Collection<Username> names = connected.values();
	private final Set<SelectionKey> keys;
	private final ByteBuffer bbBroadcast = ByteBuffer.allocateDirect(BUFFER_SIZE_BROADCAST);

	ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;
	}

	ServerSession newServerSession(SocketChannel sc, SelectionKey key) {
		ServerSession session = new ServerSession(this, sc, key);
		sessions.put(sc, session);
		return session;
	}
	
	private boolean checkAvailability(Username username) {
		return !names.contains(username);
	}

	ByteBuffer getBroadcastBuffer() {
		return bbBroadcast;
	}

	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean addNewConnected(SocketChannel sc, Username username) {
		if (!(checkAvailability(username))) {
			return false;
		}
		connected.put(sc, username);
		return true;
	}

	// TODO : Intégrer à l'ajout du broadcast
	void updateStateReadAll() {
		Logger.debug("BROADCAST : " + bbBroadcast);
		if (bbBroadcast.position() == 0) {
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

	Optional<Username> removeClient(SocketChannel channel) {
		Username disconnected = connected.remove(channel);
		return Optional.ofNullable(disconnected);
	}

	/*
	 * Returns the username associated with this SocketChannel.
	 */
	Optional<Username> usernameOf(SocketChannel sc) {
		return Optional.ofNullable(connected.get(sc));
	}
	
	Optional<ServerSession> sessionOf(SocketChannel sc) {
		return Optional.ofNullable(sessions.get(sc));
	}

	Optional<ServerSession> sessionOf(Username username) {
		Optional<SocketChannel> sc = connected.entrySet().stream()
				.filter(e -> e.getValue().equals(username))
				.map(e -> e.getKey())
				.findFirst();
		if(!sc.isPresent()) {
			return Optional.empty();
		}
		return sessionOf(sc.get());
	}

}
