package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkProtocol;

// TODO : session au lieu de database
// TODO : concurrence !!!!!
class ClientDataBase {
	private final SocketChannel publicChannel;
	private final HashMap<String, SocketChannel> privateMessages = new HashMap<>();
	private final HashMap<String, SocketChannel> privateFiles = new HashMap<>();

	ClientDataBase(SocketChannel publicChannel) {
		this.publicChannel = publicChannel;
	}

	SocketChannel getPublicChannel() {
		return publicChannel;
	}

	void addNewPrivateMessageChannel(String username, SocketChannel sc) {
		requireNonNull(username);
		requireNonNull(sc);
		privateMessages.put(username, sc);
	}
	
	void addNewPrivateFileChannel(String username, SocketChannel sc) {
		requireNonNull(username);
		requireNonNull(sc);
		privateFiles.put(username, sc);
	}

//	Optional<SocketChannel> getPrivateMessageChannel(String username) {
//		requireNonNull(username);
//		return Optional.ofNullable(privateMessages.get(username));
//	}
//
//	Optional<SocketChannel> getPrivateFileChannel(String username) {
//		requireNonNull(username);
//		return Optional.ofNullable(privateFiles.get(username));
//	}
	
	boolean sendMessage(String message) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
		return ClientCommunication.sendRequestMSG(publicChannel, message);
	}

	boolean openPrivateConnection(String username) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOREQ);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		return ClientCommunication.sendRequestPVCOREQ(publicChannel, username);
	}

	boolean acceptPrivateConnection(String username) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOACC);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		return ClientCommunication.sendRequestPVCOACC(publicChannel, username);
	}

	@SuppressWarnings("resource")
	boolean sendPrivateMessage(String username, String message) throws IOException {
		SocketChannel sc = privateMessages.get(username);
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVMSG);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
		return ClientCommunication.sendRequestPVMSG(sc, message);
	}

}
