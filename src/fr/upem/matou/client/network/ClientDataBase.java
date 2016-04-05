package fr.upem.matou.client.network;

import java.nio.channels.SocketChannel;

class ClientDataBase {
	private final SocketChannel scPublic;

	ClientDataBase(SocketChannel scPublic) {
		this.scPublic = scPublic;
	}

	SocketChannel getPublicChannel() {
		return scPublic;
	}
	
}
