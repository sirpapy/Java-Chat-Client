package fr.upem.matou.client.network;

import static fr.upem.matou.shared.logger.Logger.formatNetworkData;
import static fr.upem.matou.shared.logger.Logger.formatNetworkRequest;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;

import fr.upem.matou.client.network.ClientEvent.ClientEventConnection;
import fr.upem.matou.client.ui.UserInterface;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class represents the state of the chat client. This class is thread-safe.
 */
@SuppressWarnings("resource")
class ClientInstance implements Closeable {

	private final Object monitor = new Object();
	private final ThreadGroup threadGroup = new ThreadGroup("ChatThreads");
	private final SocketChannel sc;
	private final ClientSession session;
	private final UserInterface ui;

	private boolean exit = false;

	ClientInstance(InetSocketAddress address, UserInterface ui) throws IOException {
		this.ui = ui;
		sc = SocketChannel.open(address);
		session = new ClientSession(sc);
	}

	private void setExit() {
		synchronized (monitor) {
			exit = true;
			monitor.notifyAll();
			Logger.debug("EXIT NOTIFICATION");
		}
	}

	private void waitForTerminaison() throws InterruptedException {
		synchronized (monitor) {
			Logger.debug("WAIT FOR TERMINAISON : START WAITING");
			while (!exit) {
				monitor.wait();
			}
		}
		Logger.debug("WAIT FOR TERMINAISON : END WAITING");
	}

	private void interruptAllThreads() {
		Logger.debug("INTERRUPT THREAD GROUP");
		threadGroup.interrupt();
	}

	private boolean usernameSender(String username) throws IOException {
		ClientEvent event = new ClientEventConnection(new Username(username));
		if (!event.execute(session)) {
			ui.warnInvalidUsername(username);
			return false;
		}
		return true;
	}

	private boolean usernameReceiver(String username) throws IOException {
		NetworkProtocol protocol = ClientCommunication.receiveRequestType(sc);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PROTOCOL : " + protocol));

		switch (protocol) {

		case CORES: {
			boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "ACCEPTATION : " + acceptation));
			if (!acceptation) {
				ui.warnUnavailableUsername(username);
			}
			return acceptation;
		}

		default:
			throw new AssertionError("Unexpected protocol request : " + protocol);

		}
	}

	private boolean chatSender() throws IOException {
		Optional<ClientEvent> optional = ui.getEvent();
		if (!optional.isPresent()) {
			return true;
		}
		ClientEvent event = optional.get();

		if (!event.execute(session)) {
			ui.warnInvalidMessageEvent(event);
			return false;
		}
		return false;
	}

	/*
	 * Reads requests from the server.
	 */
	private void publicReceiver() throws IOException {
		NetworkProtocol protocol = ClientCommunication.receiveRequestType(sc);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PROTOCOL : " + protocol));

		switch (protocol) {

		case ERROR: {
			ErrorType type = ClientCommunication.receiveRequestERROR(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "ERROR : " + type));
			ui.displayError(type);

			break;
		}

		case MSGBC: {
			Message message = ClientCommunication.receiveRequestMSGBC(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + message.getUsername()));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "MESSAGE : " + message.getContent()));
			ui.displayMessage(message);

			break;
		}

		case CONOTIF: {
			Username connected = ClientCommunication.receiveRequestCONOTIF(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + connected));
			ui.displayNewConnectionEvent(connected);

			break;
		}

		case DISCONOTIF: {
			Username disconnected = ClientCommunication.receiveRequestDISCONOTIF(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + disconnected));
			ui.displayNewDisconnectionEvent(disconnected);

			break;
		}

		case PVCOREQNOTIF: {
			Username requester = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + requester));
			ui.displayNewPrivateRequestEvent(requester);

			break;
		}

		case PVCOESTASRC: {
			SourceConnection sourceInfo = ClientCommunication.receiveRequestPVCOESTASRC(sc);
			Username username = sourceInfo.getUsername();
			InetAddress address = sourceInfo.getAddress();
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + username));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "ADDRESS : " + address));
			ui.displayNewPrivateAcceptionEvent(username);
			launchPrivateConnection(username, address);

			break;
		}

		case PVCOESTADST: {
			DestinationConnection destinationInfo = ClientCommunication.receiveRequestPVCOESTADST(sc);
			Username username = destinationInfo.getUsername();
			InetAddress address = destinationInfo.getAddress();
			int portMessage = destinationInfo.getPortMessage();
			int portFile = destinationInfo.getPortFile();
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + username));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "ADDRESS : " + address));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PORT MESSAGE : " + portMessage));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PORT FILE : " + portFile));
			ui.displayNewPrivateAcceptionEvent(username);
			launchPrivateConnection(username, address, portMessage, portFile);

			break;
		}

		default:
			throw new IOException("Unsupported protocol request : " + protocol);

		}

	}

	/*
	 * Reads message requests from a client.
	 */
	private void privateMessageReceiver(SocketChannel pv, Username username) throws IOException {
		while (true) {
			NetworkProtocol protocol = ClientCommunication.receiveRequestType(pv);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PROTOCOL : " + protocol));

			switch (protocol) {

			case PVMSG: {
				Message message = ClientCommunication.receiveRequestPVMSG(pv, username);
				Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + message.getUsername()));
				Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "MESSAGE : " + message.getContent()));
				ui.displayMessage(message);

				break;
			}

			default:
				throw new IOException("Unsupported protocol request : " + protocol);

			}
		}
	}

	/*
	 * Reads file requests from a client.
	 */
	private void privateFileReceiver(SocketChannel pv, Username username) throws IOException {
		while (true) {
			NetworkProtocol protocol = ClientCommunication.receiveRequestType(pv);
			Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PROTOCOL : " + protocol));

			switch (protocol) {

			case PVFILE: {
				Path path = ClientCommunication.receiveRequestPVFILE(pv, username.toString());
				Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + username));
				Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PATH : " + path));
				ui.displayNewFileReception(username.toString(), path);

				break;
			}

			default:
				throw new IOException("Unsupported protocol request : " + protocol);

			}
		}
	}

	private void privateCommunicationSource(Username username, InetAddress addressDst) throws IOException {
		ServerSocketChannel sscMessage = ServerSocketChannel.open();
		ServerSocketChannel sscFile = ServerSocketChannel.open();
		sscMessage.bind(null);
		sscFile.bind(null);

		int portMessage = ((InetSocketAddress) sscMessage.getLocalAddress()).getPort();
		int portFile = ((InetSocketAddress) sscFile.getLocalAddress()).getPort();
		Logger.debug("[SOURCE] MESSAGE PORT : " + portMessage);
		Logger.debug("[SOURCE] FILE PORT : " + portFile);

		ClientCommunication.sendRequestPVCOPORT(sc, username.toString(), portMessage, portFile);

		new Thread(threadGroup, () -> {
			try (SocketChannel scMessage = ClientCommunication.acceptConnection(sscMessage, addressDst)) {
				Logger.debug(formatNetworkData(scMessage, "[SOURCE] MESSAGE CONNECTED"));
				session.addNewPrivateMessageChannel(username, scMessage);
				privateMessageReceiver(scMessage, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
			}
		}, "Private message receiver : " + username).start();

		new Thread(threadGroup, () -> {
			try (SocketChannel scFile = ClientCommunication.acceptConnection(sscFile, addressDst)) {
				Logger.debug(formatNetworkData(scFile, "[SOURCE] FILE CONNECTED"));
				session.addNewPrivateFileChannel(username, scFile);
				privateFileReceiver(scFile, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
			}
		}, "Private file receiver : " + username).start();

	}

	private void privateCommunicationDestination(Username username, InetAddress addressSrc, int portMessage,
			int portFile) throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		Logger.debug("[DESTINATION] MESSAGE PORT : " + portMessage);
		Logger.debug("[DESTINATION] FILE PORT : " + portFile);

		Logger.debug(formatNetworkData(scMessage, "[DESTINATION] MESSAGE CONNECTED"));
		Logger.debug(formatNetworkData(scFile, "[DESTINATION] FILE CONNECTED"));

		new Thread(threadGroup, () -> {
			try {
				session.addNewPrivateMessageChannel(username, scMessage);
				privateMessageReceiver(scMessage, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
			}
		}, "Private message receiver : " + username).start();

		new Thread(threadGroup, () -> {
			try {
				session.addNewPrivateFileChannel(username, scFile);
				privateFileReceiver(scFile, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
			}
		}, "Private file receiver : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressDst) {
		new Thread(threadGroup, () -> {
			try {
				privateCommunicationSource(username, addressDst);
			} catch (IOException e) {
				Logger.warning(e.toString());
			}
		}, "Private source connection : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressSrc, int portMessage, int portFile) {
		new Thread(threadGroup, () -> {
			try {
				privateCommunicationDestination(username, addressSrc, portMessage, portFile);
			} catch (IOException e) {
				Logger.warning(e.toString());
			}
		}, "Private destination connection : " + username).start();
	}

	private boolean connectUsername(String username) throws IOException {
		return usernameSender(username) && usernameReceiver(username);
	}

	private void processMessages() throws InterruptedException {
		Logger.debug("USER CONNECTED");

		new Thread(threadGroup, () -> {
			try {
				boolean stop = false;
				while (!Thread.interrupted() && stop == false) {
					try {
						stop = chatSender();
					} catch (IOException e) {
						Logger.error(formatNetworkData(sc, e.toString()));
						Logger.exception(e);
						return;
					}
				}
			} finally {
				setExit();
			}
		}, "Chat sender").start();

		new Thread(threadGroup, () -> {
			try {
				while (!Thread.interrupted()) {
					try {
						publicReceiver();
					} catch (IOException e) {
						Logger.error(formatNetworkData(sc, e.toString()));
						Logger.exception(e);
						return;
					}
				}
			} finally {
				setExit();
			}
		}, "Public receiver").start();

		waitForTerminaison();

		Logger.debug("DISCONNECTION");
	}

	void start() throws IOException, InterruptedException {
		while (true) {
			String username = ui.getUsername();
			if (!connectUsername(username)) {
				continue;
			}
			break;
		}

		processMessages();
	}

	void start(String username) throws IOException, InterruptedException {
		if (!connectUsername(username)) {
			return;
		}

		processMessages();
	}

	@Override
	public void close() throws IOException {
		Logger.debug(formatNetworkData(sc, "CHAT INSTANCE CLOSING"));
		sc.close();
		interruptAllThreads();
	}

}
