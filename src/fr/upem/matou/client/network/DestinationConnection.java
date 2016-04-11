package fr.upem.matou.client.network;

import java.net.InetAddress;

class DestinationConnection {
	private final String username;
	private final InetAddress address;
	private final int portMessage;
	private final int portFile;
	
	public DestinationConnection(String username, InetAddress address, int portMessage, int portFile) {
		this.username = username;
		this.address = address;
		this.portMessage = portMessage;
		this.portFile = portFile;
	}

	String getUsername() {
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
