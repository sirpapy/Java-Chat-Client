package fr.upem.matou.shared.network;

import static fr.upem.matou.shared.network.NetworkCommunication.FILENAME_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkCommunication.MESSAGE_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkCommunication.USERNAME_MAX_SIZE;
import static fr.upem.matou.shared.network.NetworkProtocol.Communicator.CLIENT;
import static fr.upem.matou.shared.network.NetworkProtocol.Communicator.SERVER;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class defines the communication protocol types that both server and client have to use in order to meet the
 * protocol requirements.
 */
public enum NetworkProtocol {

	/**
	 * Error notification.
	 */
	ERROR(SERVER, CLIENT, "ERROR_NOTIFICATION", Integer.BYTES),

	/**
	 * Connection request.
	 */
	COREQ(CLIENT, SERVER, "PUBLIC_CONNECTION_REQUEST", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Connection response.
	 */
	CORES(SERVER, CLIENT, "PUBLIC_CONNECTION_RESPONSE", Integer.BYTES, Byte.BYTES),

	/**
	 * Connection notification.
	 */
	CONOTIF(SERVER, CLIENT, "PUBLIC_CONNECTION_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Public message.
	 */
	MSG(CLIENT, SERVER, "PUBLIC_MESSAGE", Integer.BYTES, MESSAGE_MAX_SIZE),

	/**
	 * Public message forwarding.
	 */
	MSGBC(SERVER, CLIENT, "PUBLIC_MESSAGE_BROADCAST", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES, MESSAGE_MAX_SIZE),

	/**
	 * Disconnection notification.
	 */
	DISCONOTIF(SERVER, CLIENT, "PUBLIC_DISCONNECTION_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Private connection request.
	 */
	PVCOREQ(CLIENT, SERVER, "PRIVATE_CONNECTION_REQUEST", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Private connection request notification.
	 */
	PVCOREQNOTIF(SERVER, CLIENT, "PRIVATE_CONNECTION_REQUEST_NOTIFICATION", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Private connection acceptation.
	 */
	PVCOACC(CLIENT, SERVER, "PRIVATE_CONNECTION_ACCEPTATION", Integer.BYTES, USERNAME_MAX_SIZE),

	/**
	 * Private connection port transfer.
	 */
	PVCOPORT(CLIENT, SERVER, "PRIVATE_CONNECTION_PORT_TRANSFER", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES, Integer.BYTES),

	/**
	 * Private connection establishement to source.
	 */
	PVCOESTASRC(SERVER, CLIENT, "PRIVATE_CONNECTION_ESTABLISHEMENT_SOURCE", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES, 16),

	/**
	 * Private connection establishement to destination.
	 */
	PVCOESTADST(SERVER, CLIENT, "PRIVATE_CONNECTION_ESTABLISHEMENT_DESTINATION", Integer.BYTES, USERNAME_MAX_SIZE, Integer.BYTES, 16, Integer.BYTES, Integer.BYTES),

	/**
	 * Private message.
	 */
	PVMSG(CLIENT, CLIENT, "PRIVATE_MESSAGE", Integer.BYTES, MESSAGE_MAX_SIZE),

	/**
	 * Private file.
	 */
	PVFILE(CLIENT, CLIENT, "PRIVATE_FILE", Integer.BYTES, FILENAME_MAX_SIZE, Long.BYTES), /* + FileChunks */

	;

	/**
	 * Describes a network type.
	 */
	public static enum Communicator {

		/**
		 * A client
		 */
		CLIENT,

		/**
		 * A server
		 */
		SERVER;

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
		args.add(Integer.BYTES); // Size of the protocol type (minimal request size)
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
	 *            The ordinal number of the protocol.
	 * @return The protocol if the ordinal number is valid.
	 */
	public static Optional<NetworkProtocol> getProtocol(int ordinal) {
		NetworkProtocol[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		NetworkProtocol protocol = values[ordinal];
		return Optional.of(protocol);
	}

	/**
	 * Returns the size of the largest request from Communicator "source" to Communicator "target".
	 * 
	 * @param source
	 *            The sender of the request.
	 * @param target
	 *            The receiver of the request.
	 * @return The size of the largest request.
	 */
	public static int getMaxRequestSize(Communicator source, Communicator target) {
		requireNonNull(source);
		requireNonNull(target);
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.source == source && e.target == target)
				.mapToInt(e -> e.argumentSizes.stream().mapToInt(Integer::intValue).sum()).max().getAsInt();
		// Cannot be empty because of the minimal size of the protocol type
	}

	/**
	 * Returns the size of the largest request argument from Communicator "source" to Communicator "target".
	 * 
	 * @param source
	 *            The sender of the request.
	 * @param target
	 *            The receiver of the request.
	 * @return The size of the largest request argument.
	 */
	public static int getMaxArgumentSize(Communicator source, Communicator target) {
		requireNonNull(source);
		requireNonNull(target);
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.source == source && e.target == target)
				.mapToInt(e -> e.argumentSizes.stream().mapToInt(Integer::intValue).max().getAsInt()).max().getAsInt();
		// Cannot be empty because of the minimal size of the protocol type
	}

}
