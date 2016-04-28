package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.net.InetAddress;

import fr.upem.matou.shared.network.Username;

/*
 * This object describes data of an opened private request.
 */
class SourceConnection {
	private final Username username;
	private final InetAddress address;

	SourceConnection(Username username, InetAddress address) {
		requireNonNull(username);
		requireNonNull(address);
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
