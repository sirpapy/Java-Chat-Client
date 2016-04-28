package fr.upem.matou.shared.network;

import static fr.upem.matou.shared.network.NetworkCommunication.FILENAME_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkCommunication.MESSAGE_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkCommunication.USERNAME_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkProtocol.Communicator.CLIENT;
import static fr.upem.matou.shared.network.NetworkProtocol.Communicator.SERVER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;

/**
 * This class defines the communication protocol that both server and client have to meet.
 */
public enum NetworkProtocol {

	ERROR(SERVER, CLIENT, "ERROR_NOTIFICATION", Integer.BYTES),
	COREQ(CLIENT, SERVER, "PUBLIC_CONNECTION_REQUEST", Integer.BYTES, USERNAME_MAX_SIZE),
	CORES(SERVER, CLIENT, "PUBLIC_CONNECTION_RESPONSE", Integer.BYTES, Byte.BYTES),
	CONOTIF(SERVER, CLIENT, "PUBLIC_CONNECTION_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),
	MSG(CLIENT, SERVER, "PUBLIC_MESSAGE", Integer.BYTES, MESSAGE_MAX_SIZE),
	MSGBC(SERVER, CLIENT, "PUBLIC_MESSAGE_BROADCAST", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES,
			MESSAGE_MAX_SIZE),
	DISCONOTIF(SERVER, CLIENT, "PUBLIC_DISCONNECTION_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),
	PVCOREQ(CLIENT, SERVER, "PRIVATE_CONNECTION_REQUEST", Integer.BYTES, USERNAME_MAX_SIZE),
	PVCOREQNOTIF(SERVER, CLIENT, "PRIVATE_CONNECTION_REQUEST_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),
	PVCOACC(CLIENT, SERVER, "PRIVATE_CONNECTION_ACCEPTATION", Integer.BYTES, USERNAME_MAX_SIZE),
	PVCOPORT(CLIENT, SERVER, "PRIVATE_CONNECTION_PORT_TRANSFER", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES,
			Integer.BYTES),
	PVCOESTASRC(SERVER, CLIENT, "PRIVATE_CONNECTION_ESTABLISHEMENT_SOURCE", Integer.BYTES, USERNAME_MAX_SIZE,
			Integer.BYTES, 16),
	PVCOESTADST(SERVER, CLIENT, "PRIVATE_CONNECTION_ESTABLISHEMENT_DESTINATION", Integer.BYTES, USERNAME_MAX_SIZE,
			Integer.BYTES, 16, Integer.BYTES, Integer.BYTES),
	PVMSG(CLIENT, CLIENT, "PRIVATE_MESSAGE", Integer.BYTES, MESSAGE_MAX_SIZE),
	PVFILE(CLIENT, CLIENT, "PRIVATE_FILE", Integer.BYTES, FILENAME_MAX_SIZE, Long.BYTES),
	;

	static enum Communicator {
		CLIENT, SERVER;
	}

	private final Communicator source;
	private final Communicator target;
	private final String description;
	private final List<Integer> argumentSizes;

	private NetworkProtocol(Communicator source, Communicator target, String description, Integer... sizes) {
		this.source = source;
		this.target = target;
		this.description = description;
		ArrayList<Integer> args = new ArrayList<>();
		args.add(Integer.BYTES);
		for (int size : sizes) {
			args.add(size);
		}
		argumentSizes = Collections.unmodifiableList(args);
	}

	@Override
	public String toString() {
		return "[" + ordinal() + " - " + name() + " - " + source + " => " + target + " - " + description + "]";
	}

	/**
	 * Returns the protocol associated with this ordinal number.
	 *
	 * @param ordinal
	 *            The ordinal number of the protocol
	 * @return The protocol if the ordinal number is valid
	 */
	public static Optional<NetworkProtocol> getProtocol(int ordinal) {
		NetworkProtocol[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		NetworkProtocol protocol = values[ordinal];
		return Optional.of(protocol);
	}

	private static int getMaxRequestSize(Communicator source, Communicator target) {
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.source == source && e.target == target)
				.mapToInt(e -> e.argumentSizes.stream().mapToInt(Integer::intValue).sum()).max().getAsInt();
	}

	private static int getMaxArgumentSize(Communicator source, Communicator target) {
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.source == source && e.target == target)
				.mapToInt(e -> e.argumentSizes.stream().mapToInt(Integer::intValue).max().getAsInt()).max().getAsInt();
	}

	/**
	 * Returns the size of a server read buffer.
	 * 
	 * @return The size of a server read buffer.
	 */
	public static int getServerReadBufferSize() {
		int max = getMaxArgumentSize(CLIENT, SERVER);
		Logger.debug("SERVER READ BUFFER SIZE : " + max);
		return max;
	}

	/**
	 * Returns the size of a server write buffer.
	 * 
	 * @return The size of a server write buffer.
	 */
	public static int getServerWriteBufferSize() {
		int max = getMaxRequestSize(SERVER, CLIENT) * NetworkCommunication.BUFFER_MULTIPLIER;
		Logger.debug("SERVER WRITE BUFFER SIZE : " + max);
		return max;
	}

	/**
	 * Returns the size of the server broadcast buffer.
	 * 
	 * @return The size of the server broadcast buffer.
	 */
	public static int getServerBroadcastBufferSize() {
		int max = getMaxRequestSize(SERVER, CLIENT);
		Logger.debug("SERVER BROADCAST BUFFER SIZE : " + max);
		return max;
	}
}
