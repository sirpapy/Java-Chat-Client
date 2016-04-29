package fr.upem.matou.client.network;

import static fr.upem.matou.shared.logger.Logger.formatNetworkRequest;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class represents the state of the client connected to the chat server.
 * This class is thread-safe.
 */
@SuppressWarnings("resource")
class ClientSession {
	private final SocketChannel publicChannel;
	private final ConcurrentHashMap<Username, SocketChannel> privateMessages = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Username, SocketChannel> privateFiles = new ConcurrentHashMap<>();

	ClientSession(SocketChannel publicChannel) {
		this.publicChannel = publicChannel;
	}

	SocketChannel getPublicChannel() {
		return publicChannel;
	}

	void addNewPrivateMessageChannel(Username username, SocketChannel sc) {
		privateMessages.put(username, sc);
	}

	void addNewPrivateFileChannel(Username username, SocketChannel sc) {
		privateFiles.put(username, sc);
	}

	boolean sendMessage(String message) throws IOException {
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG));
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "MESSAGE : " + message));
		return ClientCommunication.sendRequestMSG(publicChannel, message);
	}

	boolean openPrivateConnection(Username username) throws IOException {
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOREQ));
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "USERNAME : " + username));
		return ClientCommunication.sendRequestPVCOREQ(publicChannel, username.toString());
	}

	boolean acceptPrivateConnection(Username username) throws IOException {
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOACC));
		Logger.info(formatNetworkRequest(publicChannel, NetworkLogType.WRITE, "USERNAME : " + username));
		return ClientCommunication.sendRequestPVCOACC(publicChannel, username.toString());
	}

	boolean sendPrivateMessage(Username username, String message) {
		SocketChannel sc = privateMessages.get(username);
		if (sc == null) {
			return false;
		}
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVMSG));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "MESSAGE : " + message));
		try {
			return ClientCommunication.sendRequestPVMSG(sc, message);
		} catch (IOException e) {
			Logger.warning(e.toString());
			Logger.exception(e);
			closePrivateConnection(username);
			return false;
		}
	}

	boolean sendPrivateFile(Username username, Path path) {
		SocketChannel sc = privateFiles.get(username);
		if (sc == null) {
			return false;
		}
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVFILE));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PATH : " + path));
		try {
			return ClientCommunication.sendRequestPVFILE(sc, path);
		} catch (IOException e) {
			Logger.warning(e.toString());
			Logger.exception(e);
			closePrivateConnection(username);
			return false;
		}
	}

	boolean closePrivateConnection(Username username) {
		SocketChannel scMessage = privateMessages.remove(username);
		SocketChannel scFile = privateFiles.remove(username);
		boolean closed = (scMessage != null) || (scFile != null);
		if (scMessage != null) {
			Logger.debug("SILENTLY CLOSE OF : " + scMessage);
			NetworkCommunication.silentlyClose(scMessage);
		}
		if (scFile != null) {
			Logger.debug("SILENTLY CLOSE OF : " + scFile);
			NetworkCommunication.silentlyClose(scFile);
		}
		return closed;
	}

}
