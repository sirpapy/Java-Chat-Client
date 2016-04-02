package fr.upem.matou.shared.network;

import static fr.upem.matou.shared.network.NetworkCommunication.*;
import static fr.upem.matou.shared.network.NetworkProtocol.ExchangeDirection.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO : Fusion NetworkProtocol et NetworkCommunication

/*
 * This class defines the communication protocol.
 */
public enum NetworkProtocol {

	COREQ(INCOMING, "PUBLIC_CONNECTION_REQUEST", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	CORES(OUTGOING, "PUBLIC_CONNECTION_RESPONSE", LENGTH_SIZE, Byte.BYTES),
	CODISP(OUTGOING, "PUBLIC_CONNECTION_NOTIFICATION", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	MSG(INCOMING, "PUBLIC_MESSAGE", LENGTH_SIZE, MESSAGE_MAX_SIZE),
	MSGBC(OUTGOING, "PUBLIC_MESSAGE_BROADCAST", LENGTH_SIZE, PSEUDO_MAX_SIZE, LENGTH_SIZE, MESSAGE_MAX_SIZE),
	DISCO(INCOMING, "PUBLIC_DISCONNECTION", LENGTH_SIZE),
	DISCODISP(OUTGOING, "PUBLIC_DISCONNECTION_NOTIFICATION", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	// PVCOREQ("CLIENT_PRIVATE_CONNECTION_REQUEST"),
	// PVCOTR("SERVER_PRIVATE_CONNECTION_TRANSFER"),
	// PVCOACC("CLIENT_PRIVATE_CONNECTION_CONFIRM"),
	// PVCORES("SERVER_PRIVATE_CONNECTION_RESPONSE"),
	// PVCOETA("SERVER_PRIVATE_CONNECTION_ETABLISHMENT"),
	// PVMSG("CLIENT_PRIVATE_MESSAGE"),
	// PVFILE("CLIENT_PRIVATE_FILE"),
	// PVDISCO("CLIENT_PRIVATE_DISCONNECTION"),
	;

	static enum ExchangeDirection {
		INCOMING, OUTGOING;
	}

	private final ExchangeDirection direction;
	private final String description;
	private final List<Integer> argumentSizes;
	private final int maxRequestSize;

	private NetworkProtocol(ExchangeDirection direction, String description, Integer... sizes) {
		this.direction = direction;
		this.description = description;
		ArrayList<Integer> args = new ArrayList<>();
		args.add(Integer.BYTES);
		for (int size : sizes) {
			args.add(size);
		}
		argumentSizes = Collections.unmodifiableList(args);
		maxRequestSize = argumentSizes.stream().mapToInt(Integer::intValue).sum();
	}

	public int getMaxRequestSize() {
		return maxRequestSize;
	}

	public int getArgumentSize(int arg) {
		return argumentSizes.get(arg);
	}

	@Override
	public String toString() {
		String dir = "";

		switch (direction) {
		case INCOMING:
			dir = "CLIENT";
			break;
		case OUTGOING:
			dir = "SERVER";
			break;
		default:
			break;
		}

		return "[" + ordinal() + " - " + name() + " - " + dir + " " + description + "]";
	}

	/*
	 * Returns the protocol from the ordinal number.
	 */
	public static Optional<NetworkProtocol> getProtocol(int ordinal) {
		NetworkProtocol[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		NetworkProtocol protocol = values[ordinal];
		return Optional.of(protocol);
	}

	private static int getMaxRequestSize(ExchangeDirection direction) {
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.direction == direction).mapToInt(e -> e.maxRequestSize).max()
				.getAsInt();
	}

	public static int getMaxIncomingRequestSize() {
		return getMaxRequestSize(INCOMING);
	}

	public static int getMaxOutgoingRequestSize() {
		return getMaxRequestSize(OUTGOING);
	}
}
