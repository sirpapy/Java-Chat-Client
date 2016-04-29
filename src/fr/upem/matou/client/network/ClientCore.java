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

import fr.upem.matou.client.ui.ShellInterface;
import fr.upem.matou.client.ui.UserInterface;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/**
 * This class is the core of the chat client.
 */
@SuppressWarnings("resource")
public class ClientCore implements Closeable {

	private final Object monitor = new Object();
	private final SocketChannel sc;
	private final ClientSession session;
	private final UserInterface ui = new ShellInterface();

	private boolean exit;

	/**
	 * Constructs a new client core.
	 * 
	 * @param hostname
	 *            The server hostname.
	 * @param port
	 *            The serveur pore.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
		session = new ClientSession(sc);
	}

	private void setExit() {
		synchronized (monitor) {
			Logger.debug("EXIT !!!");
			exit = true;
			monitor.notify();
		}
	}

	private void waitForTerminaison() throws InterruptedException {
		synchronized (monitor) {
			while (!exit) {
				monitor.wait();
			}
		}
	}

	private static void interruptAllThreads() {
		Thread.currentThread().getThreadGroup().interrupt();
	}

	private Optional<String> usernameGetter() {
		return ui.getUsername();
	}

	private boolean usernameSender(String username) throws IOException {
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.COREQ));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));
		if (!ClientCommunication.sendRequestCOREQ(sc, username)) {
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

	private boolean messageSender() throws IOException {
		Optional<ClientEvent> optional = ui.getEvent();
		if (!optional.isPresent()) {
			return true;
		}
		ClientEvent event = optional.get();

		if (!event.execute(session)) {
			ui.warnInvalidEvent(event);
			return false;
		}
		return false;
	}

	/*
	 * Reads requests from the server.
	 */
	private void messageReceiver() throws IOException {
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
	private void privateCommunicationMessage(SocketChannel pv, Username username) throws IOException {
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
	private void privateCommunicationFile(SocketChannel pv, Username username) throws IOException {
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

	private static SocketChannel acceptCommunication(ServerSocketChannel ssc, InetAddress address) throws IOException {
		try (ServerSocketChannel listening = ssc) {
			SocketChannel pv;
			while (true) {
				pv = ssc.accept();
				InetAddress connected = ((InetSocketAddress) pv.getRemoteAddress()).getAddress();
				if (!address.equals(connected)) {
					Logger.debug(formatNetworkData(pv, "[SOURCE] CONNECTION HIJACK !!!"));
					NetworkCommunication.silentlyClose(pv);
					continue;
				}
				Logger.debug(formatNetworkData(pv,"[SOURCE] CONNECTION ACCEPTED"));
				return pv;
			}
		} // close the ssc correctly
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

		new Thread(() -> {
			try (SocketChannel scMessage = acceptCommunication(sscMessage, addressDst)) {
				Logger.debug(formatNetworkData(scMessage,"[SOURCE] MESSAGE CONNECTED"));
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
			}
		}, "Private messages : " + username).start();

		new Thread(() -> {
			try (SocketChannel scFile = acceptCommunication(sscFile, addressDst)) {
				Logger.debug(formatNetworkData(scFile,"[SOURCE] FILE CONNECTED"));
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
			}
		}, "Private files : " + username).start();

	}

	private void privateCommunicationDestination(Username username, InetAddress addressSrc, int portMessage,
			int portFile) throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		Logger.debug("[DESTINATION] MESSAGE PORT : " + portMessage);
		Logger.debug("[DESTINATION] FILE PORT : " + portFile);

		Logger.debug(formatNetworkData(scMessage,"[DESTINATION] MESSAGE CONNECTED"));
		Logger.debug(formatNetworkData(scFile,"[DESTINATION] FILE CONNECTED"));

		new Thread(() -> {
			try {
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
			}
		}, "Private messages : " + username).start();

		new Thread(() -> {
			try {
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
			}
		}, "Private files : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressDst) {
		new Thread(() -> {
			try {
				privateCommunicationSource(username, addressDst);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
			}
		}, "Private source connection : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressSrc, int portMessage, int portFile) {
		new Thread(() -> {
			try {
				privateCommunicationDestination(username, addressSrc, portMessage, portFile);
			} catch (IOException e) {
				Logger.warning(e.toString());
				Logger.exception(e);
			}
		}, "Private destination connection : " + username).start();
	}

	private void threadMessageSender() {
		try {
			boolean stop = false;
			while (!Thread.interrupted() && stop == false) {
				try {
					stop = messageSender();
				} catch (IOException e) {
					Logger.warning(e.toString());
					Logger.exception(e);
					return;
				}
			}
		} finally {
			setExit();
		}
	}

	private void threadMessageReceiver() {
		try {
			while (!Thread.interrupted()) {
				try {
					messageReceiver();
				} catch (IOException e) {
					Logger.warning(e.toString());
					Logger.exception(e);
					return;
				}
			}
		} finally {
			setExit();
		}
	}

	private boolean connectUsername(String username) throws IOException {
		return usernameSender(username) && usernameReceiver(username);
	}

	private void processMessages() throws InterruptedException {
		Thread sender = new Thread(() -> threadMessageSender(), "Public sender");
		Thread receiver = new Thread(() -> threadMessageReceiver(), "Public receiver");

		sender.start();
		receiver.start();

		waitForTerminaison();

		interruptAllThreads();
	}

	/**
	 * Starts a chat without a predefined username. User will have to choose a username manually.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws InterruptedException
	 *             If an interruption occurs.
	 */
	public void startChat() throws IOException, InterruptedException {
		exit = false;
		while (true) {
			Optional<String> optional = usernameGetter();
			if (!optional.isPresent()) {
				Logger.debug("CONNECTION FAILED");
				return;
			}
			String username = optional.get();
			if (!usernameSender(username)) {
				continue;
			}
			if (!usernameReceiver(username)) {
				continue;
			}
			break;
		}

		processMessages();

		Logger.debug("DISCONNECTION");
	}

	/**
	 * Starts a chat with a predefined username.
	 * 
	 * @param username
	 *            The choosen username.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws InterruptedException
	 *             If an interruption occurs.
	 */
	public void startChat(String username) throws IOException, InterruptedException {
		exit = false;
		if (!connectUsername(username)) {
			Logger.debug("CONNECTION FAILED");
			return;
		}

		processMessages();

		Logger.debug("DISCONNECTION");
	}

	/**
	 * Starts a chat with a predefined username.
	 * 
	 * @param username
	 *            An optional describing a username.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws InterruptedException
	 *             If an interruption occurs.
	 */
	public void startChat(Optional<String> username) throws IOException, InterruptedException {
		if (username.isPresent()) {
			startChat(username.get());
		} else {
			startChat();
		}
	}

	@Override
	public void close() throws IOException {
		Logger.debug("CHAT CLOSING");
		ui.close();
		sc.close();
	}

}
