package fr.upem.matou.tcp;

import java.util.Optional;

/*
 * This class defines the communication protocol.
 */
enum NetworkProtocol {
	COREQ("CLIENT_PUBLIC_CONNECTION_REQUEST"),
	CORES("SERVER_PUBLIC_CONNECTION_RESPONSE"),
	CODISP("SERVER_PUBLIC_CONNECTION_NOTIFICATION"),
	MSG("CLIENT_PUBLIC_MESSAGE"),
	MSGBC("SERVER_PUBLIC_MESSAGE_BROADCAST"),
	// PVCOREQ("CLIENT_PRIVATE_CONNECTION_REQUEST"),
	// PVCOTR("SERVER_PRIVATE_CONNECTION_TRANSFER"),
	// PVCOACC("CLIENT_PRIVATE_CONNECTION_CONFIRM"),
	// PVCORES("SERVER_PRIVATE_CONNECTION_RESPONSE"),
	// PVCOETA("SERVER_PRIVATE_CONNECTION_ETABLISHMENT"),
	// PVMSG("CLIENT_PRIVATE_MESSAGE"),
	// PVFILE("CLIENT_PRIVATE_FILE"),
	// PVDISCO("CLIENT_PRIVATE_DISCONNECTION"),
	DISCO("CLIENT_PUBLIC_DISCONNECTION"),
	;

	private final String description;

	private NetworkProtocol(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "[" + ordinal() + " - " + name() + " - " + description + "]";
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
