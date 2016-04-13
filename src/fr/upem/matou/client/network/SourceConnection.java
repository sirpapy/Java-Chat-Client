package fr.upem.matou.client.network;

import java.net.InetAddress;

import fr.upem.matou.shared.network.Username;

class SourceConnection {
	private final Username username;
	private final InetAddress address;

	public SourceConnection(String username, InetAddress address) {
		this.username = new Username(username);
		this.address = address;
	}

	Username getUsername() {
		return username;
	}

	InetAddress getAddress() {
		return address;
	}

}
