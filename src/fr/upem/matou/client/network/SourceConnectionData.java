package fr.upem.matou.client.network;

import java.net.InetAddress;

import fr.upem.matou.shared.network.Username;

/*
 * This object describes data of an opened private request.
 */
class SourceConnectionData {
	private final Username username;
	private final InetAddress address;

	SourceConnectionData(Username username, InetAddress address) {
		this.username = username;
		this.address = address;
	}

	Username getUsername() {
		return username;
	}

	InetAddress getAddress() {
		return address;
	}

}
