package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkProtocol;

// TODO : session au lieu de database
class ClientDataBase {
	private final SocketChannel publicChannel;
	private final HashMap<String, SocketChannel> privateChannels = new HashMap<>();

	ClientDataBase(SocketChannel publicChannel) {
		this.publicChannel = publicChannel;
	}

	SocketChannel getPublicChannel() {
		return publicChannel;
	}

	void addNewPrivateChannel(String pseudo, SocketChannel sc) {
		requireNonNull(pseudo);
		requireNonNull(sc);
		privateChannels.put(pseudo, sc);
	}

	Optional<SocketChannel> getPrivateChannel(String pseudo) {
		requireNonNull(pseudo);
		return Optional.ofNullable(privateChannels.get(pseudo));
	}

	boolean sendMessage(String message) throws IOException {
		SocketChannel sc = publicChannel;
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
		return ClientCommunication.sendRequestMSG(sc, message);
	}

}
