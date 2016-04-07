package fr.upem.matou.shared.network;

import static fr.upem.matou.shared.network.NetworkCommunication.*;
import static fr.upem.matou.shared.network.NetworkProtocol.Communicator.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/*
 * This class defines the communication protocol.
 */
public enum NetworkProtocol {

	COREQ(CLIENT, SERVER, "PUBLIC_CONNECTION_REQUEST", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	CORES(SERVER, CLIENT, "PUBLIC_CONNECTION_RESPONSE", LENGTH_SIZE, Byte.BYTES),
	CODISP(SERVER, CLIENT, "PUBLIC_CONNECTION_NOTIFICATION", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	MSG(CLIENT, SERVER, "PUBLIC_MESSAGE", LENGTH_SIZE, MESSAGE_MAX_SIZE),
	MSGBC(SERVER, CLIENT, "PUBLIC_MESSAGE_BROADCAST", LENGTH_SIZE, PSEUDO_MAX_SIZE, LENGTH_SIZE, MESSAGE_MAX_SIZE),
	DISCO(CLIENT, SERVER, "PUBLIC_DISCONNECTION", LENGTH_SIZE),
	DISCODISP(SERVER, CLIENT, "PUBLIC_DISCONNECTION_NOTIFICATION", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	PVCOREQ(CLIENT, SERVER, "PRIVATE_CONNECTION_REQUEST", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	PVCODISP(SERVER, CLIENT, "PRIVATE_CONNECTION_NOTIFICATION", LENGTH_SIZE, PSEUDO_MAX_SIZE),
	PVCOACC(CLIENT,SERVER, "PRIVATE_CONNECTION_ACCEPTATION",LENGTH_SIZE, PSEUDO_MAX_SIZE),
	// PVCOETA("SERVER_PRIVATE_CONNECTION_ETABLISHMENT"),
	// PVMSG("CLIENT_PRIVATE_MESSAGE"),
	// PVFILE("CLIENT_PRIVATE_FILE"),
	// PVDISCO("CLIENT_PRIVATE_DISCONNECTION"),
	;

	static enum Communicator {
		CLIENT, SERVER;
	}

	private final Communicator source;
	private final Communicator target;
	private final String description;
	private final List<Integer> argumentSizes;
	private final int maxRequestSize;

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

		switch (source) {
		case CLIENT:
			dir = "CLIENT";
			break;
		case SERVER:
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

	private static int getMaxRequestSize(Communicator source, Communicator target) {
		NetworkProtocol[] values = values();
		return Arrays.stream(values).filter(e -> e.source == source && e.target == target)
				.mapToInt(e -> e.maxRequestSize).max().getAsInt();
	}

	public static int getMaxClientToServerRequestSize() {
		int x = getMaxRequestSize(CLIENT, SERVER);
		System.out.println("MAX CLIENT TO SERVER : " + x);
		return x;
	}

	public static int getMaxServerToClientRequestSize() {
		int x = getMaxRequestSize(SERVER, CLIENT);
		System.out.println("MAX SERVER TO CLIENT : " + x);
		return x;
	}
}
