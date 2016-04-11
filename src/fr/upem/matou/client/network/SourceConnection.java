package fr.upem.matou.client.network;

import java.net.InetAddress;

class SourceConnection {
	private final String username;
	private final InetAddress address;
	
	public SourceConnection(InetAddress address, String username) {
		this.address = address;
		this.username = username;
	}

	String getUsername() {
		return username;
	}
	
	InetAddress getAddress() {
		return address;
	}

}
