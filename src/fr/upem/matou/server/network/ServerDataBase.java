package fr.upem.matou.server.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class represents the state of the chat server.
 * This class is not thread-safe and should not be used by several threads.
 */
class ServerDataBase {
	private static final int BUFFER_SIZE_BROADCAST = NetworkProtocol.getMaxServerToClientRequestSize();

	private final HashMap<SocketChannel, ServerSession> sessions = new HashMap<>();
	private final HashMap<SocketChannel, Username> connected = new HashMap<>();
	private final HashMap<Username, HashSet<Username>> privateRequests = new HashMap<>();
	private final Collection<Username> names = connected.values();
	private final Set<SelectionKey> keys;
	private final ByteBuffer bbBroadcast = ByteBuffer.allocateDirect(BUFFER_SIZE_BROADCAST);

	ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;
	}

	ServerSession newServerSession(SocketChannel sc, SelectionKey key) throws IOException {
		ServerSession session = new ServerSession(this, sc, key);
		sessions.put(sc, session);
		return session;
	}

	/*
	 * Add a new client.
	 * Check if : available & no illegal characters
	 */
	boolean authentClient(SocketChannel sc, Username username) {
		if (!(checkAvailability(username))) {
			return false;
		}
		connected.put(sc, username);
		return true;
	}

	Optional<Username> removeClient(SocketChannel channel) {
		sessions.remove(channel);
		Username disconnected = connected.remove(channel);
		if (disconnected != null) {
			privateRequests.remove(disconnected);
			removeAllRequestTo(disconnected);
		}
		return Optional.ofNullable(disconnected);
	}

	private void removeAllRequestTo(Username disconnected) {
		for (Entry<Username, HashSet<Username>> entry : privateRequests.entrySet()) {
			Username key = entry.getKey();
			HashSet<Username> values = entry.getValue();
			boolean cancelled = values.remove(disconnected);
			if (cancelled) {
				Logger.debug("Cancel private request : " + key + " -> " + disconnected);
			}
		}
	}

	private boolean checkAvailability(Username username) {
		return !names.contains(username);
	}

	ByteBuffer getBroadcastBuffer() {
		return bbBroadcast;
	}

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
		if (!sc.isPresent()) {
			return Optional.empty();
		}
		return sessionOf(sc.get());
	}

	boolean addNewPrivateRequest(Username source, Username target) {
		HashSet<Username> set = privateRequests.get(source);
		if (set == null) {
			set = new HashSet<>();
			privateRequests.put(source, set);
		}
		boolean added = set.add(target);
		Logger.debug("SET : " + set);
		return added;
	}

	boolean checkPrivateRequest(Username source, Username target) {
		HashSet<Username> set = privateRequests.get(target);
		Logger.debug("CHECK ( " + source + " -> " + target + " ) = " + set);
		if (set == null) {
			return false;
		}
		return set.contains(source);
	}

	boolean removePrivateRequest(Username source, Username target) {
		HashSet<Username> set = privateRequests.get(target);
		Logger.debug("CHECK & REMOVE ( " + source + " -> " + target + " ) = " + set);
		if (set == null) {
			return false;
		}
		return set.remove(source);
	}

}
