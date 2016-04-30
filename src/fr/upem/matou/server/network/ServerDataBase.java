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
import fr.upem.matou.shared.network.Username;

/*
 * This class represents the state of the chat server. This class is not thread-safe and should not be used by several
 * threads.
 */
class ServerDataBase {

	private static final int MAX_CLIENT = 10;
	private static final int BUFFER_SIZE_BROADCAST = ServerCommunication.getServerBroadcastBufferSize();

	private final HashMap<SocketChannel, ServerSession> sessions = new HashMap<>();
	private final HashMap<SocketChannel, Username> authenticated = new HashMap<>(); // All authenticated usernames
	private final HashMap<Username, HashSet<Username>> privateRequests = new HashMap<>();
	private final Collection<Username> names = authenticated.values();
	private final Set<SelectionKey> keys;
	private final ByteBuffer bbBroadcast = ByteBuffer.allocateDirect(BUFFER_SIZE_BROADCAST);

	ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;
	}

	/*
	 * Creates a new ServerSession in this ServerDataBase.
	 */
	Optional<ServerSession> newServerSession(SocketChannel sc, SelectionKey key) throws IOException { // O(1)
		if (sessions.size() >= MAX_CLIENT) {
			return Optional.empty();
		}
		ServerSession session = new ServerSession(this, sc, key);
		sessions.put(sc, session);
		return Optional.of(session);
	}

	/*
	 * Returns the common byte buffer of all clients.
	 */
	ByteBuffer getBroadcastBuffer() {
		return bbBroadcast;
	}
	
	/*
	 * Checks if a username is available.
	 */
	private boolean checkAvailability(Username username) { // O(n)
		return !names.contains(username);
	}

	/*
	 * Adds a new client. Check if this username is available and does not contain any illegal character.
	 */
	boolean authentClient(SocketChannel sc, Username username) { // O(n)
		if (!(checkAvailability(username))) {
			return false;
		}
		authenticated.put(sc, username);
		return true;
	}

	/*
	 * Removes all private request of connected clients to a specific client.
	 */
	private void removeAllRequestTo(Username disconnected) { // O(n)
		for (Entry<Username, HashSet<Username>> entry : privateRequests.entrySet()) {
			Username key = entry.getKey();
			HashSet<Username> values = entry.getValue();
			boolean cancelled = values.remove(disconnected);
			if (cancelled) {
				Logger.debug("Cancel private request : " + key + " -> " + disconnected);
			}
		}
	}

	/*
	 * Removes a client from the database.
	 */
	Optional<Username> removeClient(SocketChannel channel) { // O(n)
		sessions.remove(channel);
		Username disconnected = authenticated.remove(channel);
		if (disconnected != null) {
			privateRequests.remove(disconnected);
			removeAllRequestTo(disconnected);
		}
		return Optional.ofNullable(disconnected);
	}

	/*
	 * Returns the username associated with this SocketChannel.
	 */
	Optional<Username> usernameOf(SocketChannel sc) { // O(1)
		return Optional.ofNullable(authenticated.get(sc));
	}

	/*
	 * Returns the session associated with this SocketChannel.
	 */
	Optional<ServerSession> sessionOf(SocketChannel sc) { // O(1)
		return Optional.ofNullable(sessions.get(sc));
	}

	/*
	 * Returns the session associated with this username.
	 */
	Optional<ServerSession> sessionOf(Username username) { // O(n)
		Optional<SocketChannel> sc = authenticated.entrySet().stream().filter(e -> e.getValue().equals(username))
				.map(e -> e.getKey()).findFirst();
		if (!sc.isPresent()) {
			return Optional.empty();
		}
		return sessionOf(sc.get());
	}

	/*
	 * Adds a new private request.
	 */
	boolean addPrivateRequest(Username source, Username target) { // O(1)
		HashSet<Username> set = privateRequests.get(source);
		if (set == null) {
			set = new HashSet<>();
			privateRequests.put(source, set);
		}
		boolean added = set.add(target);
		Logger.debug("PV ADD (" + source + " -> " + target + ") : " + set);
		return added;
	}

	/*
	 * Checks if source has requested the target for private connection.
	 */
	boolean checkPrivateRequest(Username source, Username target) { // O(1)
		HashSet<Username> set = privateRequests.get(target);
		Logger.debug("PV CHECK (" + source + " -> " + target + ") : " + set);
		if (set == null) {
			return false;
		}
		return set.contains(source);
	}

	/*
	 * Removes a private request.
	 */
	boolean removePrivateRequest(Username source, Username target) { // O(1)
		HashSet<Username> set = privateRequests.get(target);
		Logger.debug("PV CHECK & REMOVE (" + source + " -> " + target + ") : " + set);
		if (set == null) {
			return false;
		}
		return set.remove(source);
	}
	
	/*
	 * Updates the read state of all clients. If the broadcast bytebuffer is not empty, then all the client's write
	 * bytebuffer are filled by the broadcast bytebuffer. The broadcast bytebuffer is cleared after this operation.
	 */
	void updateStateReadAll() { // O(n)
		Logger.debug("BROADCAST BUFFER : " + bbBroadcast);
		if (bbBroadcast.position() == 0) {
			return;
		}

		int ready = 0;
		for (SelectionKey key : keys) {
			if (!key.isValid()) {
				continue;
			}

			ServerSession session = (ServerSession) key.attachment();
			if (session == null) {
				continue;
			}

			if (!session.isAuthent()) {
				continue;
			}

			session.appendWriteBuffer(bbBroadcast);

			int ops = key.interestOps();
			key.interestOps(ops | SelectionKey.OP_WRITE);

			ready++;
		}
		Logger.debug("Forwarding to " + ready + " client(s)");

		bbBroadcast.clear();
	}

}
