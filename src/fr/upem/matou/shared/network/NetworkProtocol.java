package fr.upem.matou.shared.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// TODO : Fusion NetworkProtocol et NetworkCommunication

/*
 * This class defines the communication protocol.
 */
public enum NetworkProtocol {
	
	
	COREQ(ExchangeDirection.INCOMING, "PUBLIC_CONNECTION_REQUEST", 4, 32),
	CORES(ExchangeDirection.OUTGOING, "PUBLIC_CONNECTION_RESPONSE", 4, 1),
	CODISP(ExchangeDirection.OUTGOING, "PUBLIC_CONNECTION_NOTIFICATION", 4, 32),
	MSG(ExchangeDirection.INCOMING, "PUBLIC_MESSAGE", 4, 512),
	MSGBC(ExchangeDirection.OUTGOING, "PUBLIC_MESSAGE_BROADCAST", 4, 32, 4, 512),
	DISCO(ExchangeDirection.INCOMING, "PUBLIC_DISCONNECTION", 4),
	DISCODISP(ExchangeDirection.OUTGOING, "PUBLIC_DISCONNECTION_NOTIFICATION", 4, 32),
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

	int getMaxRequestSize() {
		return maxRequestSize;
	}

	int getArgumentSize(int arg) {
		return argumentSizes.get(arg);
	}

	@Override
	public String toString() {
		String dir = "";
		
		switch(direction) {
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

}
