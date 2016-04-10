package fr.upem.matou.client.network;

import java.net.InetAddress;

class DestinationConnection {
	private final InetAddress address;
	private final int portMessage;
	private final int portFile;
	
	public DestinationConnection(InetAddress address, int portMessage, int portFile) {
		this.address = address;
		this.portMessage = portMessage;
		this.portFile = portFile;
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
