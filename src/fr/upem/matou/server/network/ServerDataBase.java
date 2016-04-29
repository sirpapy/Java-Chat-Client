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
	
	private static final int MAX_CLIENT = 2; // TEMP : Augmenter
	private static final int BUFFER_SIZE_BROADCAST = NetworkProtocol.getServerBroadcastBufferSize();

	private final HashMap<SocketChannel, ServerSession> sessions = new HashMap<>();
	private final HashMap<SocketChannel, Username> connected = new HashMap<>();
	private final HashMap<Username, HashSet<Username>> privateRequests = new HashMap<>();
	private final Collection<Username> names = connected.values();
	private final Set<SelectionKey> keys;
	private final ByteBuffer bbBroadcast = ByteBuffer.allocateDirect(BUFFER_SIZE_BROADCAST);
	
	ServerDataBase(Set<SelectionKey> keys) {
		this.keys = keys;		
	}

	/*
	 * Creates a new ServerSession in this ServerDataBase.
	 */
	Optional<ServerSession> newServerSession(SocketChannel sc, SelectionKey key) throws IOException {
		if(sessions.size() >= MAX_CLIENT) {
			return Optional.empty();
		}
		ServerSession session = new ServerSession(this, sc, key);
		sessions.put(sc, session);
		return Optional.of(session);
	}
	
	/*
	 * Checks if a username is available.
	 */
	private boolean checkAvailability(Username username) {
		return !names.contains(username);
	}

	/*
	 * Adds a new client.
	 * Check if the username is available and does not contain any illegal character.
	 */
	boolean authentClient(SocketChannel sc, Username username) {
		if (!(checkAvailability(username))) {
			return false;
		}
		connected.put(sc, username);
		return true;
	}

	/*
	 * Removes all private request of connected clients to a specific client.
	 */
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
	
	/*
	 * Removes a client from the database.
	 */
	Optional<Username> removeClient(SocketChannel channel) {
		sessions.remove(channel);
		Username disconnected = connected.remove(channel);
		if (disconnected != null) {
			privateRequests.remove(disconnected);
			removeAllRequestTo(disconnected);
		}
		return Optional.ofNullable(disconnected);
	}

	/*
	 * Returns the common byte buffer of all clients.
	 */
	ByteBuffer getBroadcastBuffer() {
		return bbBroadcast;
	}

	/*
	 * Updates the read state of all clients.
	 * If the broadcast bytebuffer is not empty, then all the client's write bytebuffer are filled by the broadcast
	 * bytebuffer.
	 * The broadcast bytebuffer is cleared after this operation.
	 */
	void updateStateReadAll() {
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

	/*
	 * Returns the username associated with this SocketChannel.
	 */
	Optional<Username> usernameOf(SocketChannel sc) {
		return Optional.ofNullable(connected.get(sc));
	}

	/*
	 * Returns the session associated with this SocketChannel.
	 */
	Optional<ServerSession> sessionOf(SocketChannel sc) {
		return Optional.ofNullable(sessions.get(sc));
	}

	/*
	 * Returns the session associated with this username.
	 */
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

	/*
	 * Adds a new private request.
	 */
	boolean addNewPrivateRequest(Username source, Username target) {
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
	boolean checkPrivateRequest(Username source, Username target) {
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
	boolean removePrivateRequest(Username source, Username target) {
		HashSet<Username> set = privateRequests.get(target);
		Logger.debug("PV CHECK & REMOVE (" + source + " -> " + target + ") : " + set);
		if (set == null) {
			return false;
		}
		return set.remove(source);
	}

}
