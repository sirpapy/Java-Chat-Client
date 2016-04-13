package fr.upem.matou.client.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

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
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
		return ClientCommunication.sendRequestMSG(publicChannel, message);
	}

	boolean openPrivateConnection(Username username) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOREQ);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		return ClientCommunication.sendRequestPVCOREQ(publicChannel, username.toString());
	}

	boolean acceptPrivateConnection(Username username) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOACC);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		return ClientCommunication.sendRequestPVCOACC(publicChannel, username.toString());
	}

	boolean sendPrivateMessage(Username username, String message) throws IOException {
		SocketChannel sc = privateMessages.get(username);
		if (sc == null) {
			return false;
		}
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVMSG);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);
		return ClientCommunication.sendRequestPVMSG(sc, message);
	}

	boolean sendPrivateFile(Username username, Path path) throws IOException {
		SocketChannel sc = privateFiles.get(username);
		if (sc == null) {
			return false;
		}
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVFILE);
		Logger.network(NetworkLogType.WRITE, "PATH : " + path);
		return ClientCommunication.sendRequestPVFILE(sc, path);
	}

	boolean closePrivateConnection(Username username) {
		SocketChannel scMessage = privateMessages.remove(username);
		SocketChannel scFile = privateFiles.remove(username);
		boolean closed = (scMessage != null) || (scFile != null);
		if (scMessage != null) {
			try {
				scMessage.close();
			} catch (IOException e) {
				Logger.exception(e);
			}
		}
		if (scFile != null) {
			try {
				scFile.close();
			} catch (IOException e) {
				Logger.exception(e);
			}
		}
		return closed;

		// FIXME : Si la connexion est fermée, l'autre peut encore écrire une fois avant de se manger une IOException
	}

}
