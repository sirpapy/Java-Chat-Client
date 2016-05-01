package fr.upem.matou.client.network;

import java.net.InetAddress;

import fr.upem.matou.shared.network.Username;

/*
 * This object describes data of a private connection as a destination (the requested of the connection). These data are
 * about the source (the requester).
 */
class DestinationConnectionData {
	private final Username username;
	private final InetAddress address;
	private final int portMessage;
	private final int portFile;

	DestinationConnectionData(Username username, InetAddress address, int portMessage, int portFile) {
		this.username = username;
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
