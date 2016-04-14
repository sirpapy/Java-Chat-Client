package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.net.InetAddress;

import fr.upem.matou.shared.network.Username;

/*
 * This object describes data of an accepted private request.
 */
class DestinationConnection {
	private final Username username;
	private final InetAddress address;
	private final int portMessage;
	private final int portFile;

	DestinationConnection(String username, InetAddress address, int portMessage, int portFile) {
		requireNonNull(username);
		requireNonNull(address);
		this.username = new Username(username);
		this.address = address;
		this.portMessage = portMessage;
		this.portFile = portFile;
	}

	Username getUsername() {
		return username;
	}

	InetAddress getAddress() {
		return address;
	}

	int getPortMessage() {
		return portMessage;
	}

	int getPortFile() {
		return portFile;
	}

}
