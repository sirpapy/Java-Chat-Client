package fr.upem.matou.client.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import fr.upem.matou.client.ui.ShellInterface;
import fr.upem.matou.client.ui.UserInterface;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class is the core of the client.
 */
@SuppressWarnings("resource")
public class ClientCore implements Closeable {

	private final Object monitor = new Object();
	private boolean exit = false;
	private final SocketChannel sc;
	private final ClientSession session;
	private final UserInterface ui = new ShellInterface();
	private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
		session = new ClientSession(sc);
	}

	private void registerThread() {
		Thread thread = Thread.currentThread();
		Logger.debug("Register Thread : " + thread);
		threads.add(thread);
	}

	private void unregisterThread() {
		Thread thread = Thread.currentThread();
		Logger.debug("Unregister Thread : " + thread);
		threads.remove(thread);
	}

	private void setExit() {
		Logger.debug("EXIT !!!");
		synchronized (monitor) {
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

	// FIXME : ExecutorService OR ThreadGroup
	private void interruptAllThreads() {
		ArrayList<Thread> privates = new ArrayList<>(threads);
		Set<Thread> before = Thread.getAllStackTraces().keySet();
		Logger.debug("Working BEFORE : " + before);
		// for (Thread thread : privates) {
		// Logger.debug("Interrupt thread : " + thread);
		// thread.interrupt();
		// }
		Thread.currentThread().getThreadGroup().interrupt();
		Set<Thread> after = Thread.getAllStackTraces().keySet();
		Logger.debug("Working AFTER : " + after);
	}

	private Optional<String> usernameGetter() {
		return ui.getUsername();
	}

	private boolean usernameSender(String username) throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.COREQ);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		if (!ClientCommunication.sendRequestCOREQ(sc, username)) {
			ui.warnInvalidUsername(username);
			return false;
		}
		return true;
	}

	private boolean usernameReceiver(String username) throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol violation");
		}

		NetworkProtocol protocol = optionalRequestType.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);
		switch (protocol) {
		case CORES: {
			boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
			Logger.network(NetworkLogType.READ, "ACCEPTATION : " + acceptation);
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
			ui.warnInvalidMessage(event);
			return false;
		}
		return false;
	}

	/*
	 * Reads requests from the server.
	 */
	private void messageReceiver() throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol violation");
		}
		NetworkProtocol protocol = optionalRequestType.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

		switch (protocol) {

		case ERROR: {
			Optional<ErrorType> optionalERROR = ClientCommunication.receiveRequestERROR(sc);
			if (!optionalERROR.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			ErrorType type = optionalERROR.get();
			Logger.network(NetworkLogType.READ, "ERROR : " + type);
			ui.displayError(type);

			break;
		}

		case MSGBC: {
			Message message = ClientCommunication.receiveRequestMSGBC(sc);
			Logger.network(NetworkLogType.READ, "USERNAME : " + message.getUsername());
			Logger.network(NetworkLogType.READ, "MESSAGE : " + message.getContent());
			ui.displayMessage(message);

			break;
		}

		case CONOTIF: {
			String connected = ClientCommunication.receiveRequestCONOTIF(sc);
			Logger.network(NetworkLogType.READ, "USERNAME : " + connected);
			ui.displayNewConnectionEvent(connected);

			break;
		}

		case DISCONOTIF: {
			String disconnected = ClientCommunication.receiveRequestDISCONOTIF(sc);
			Logger.network(NetworkLogType.READ, "USERNAME : " + disconnected);
			ui.displayNewDisconnectionEvent(disconnected);

			break;
		}

		case PVCOREQNOTIF: {
			String requester = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);
			Logger.network(NetworkLogType.READ, "USERNAME : " + requester);
			ui.displayNewPrivateRequestEvent(requester);

			break;
		}

		case PVCOESTASRC: {
			SourceConnection sourceInfo = ClientCommunication.receiveRequestPVCOESTASRC(sc);
			Username username = sourceInfo.getUsername();
			InetAddress address = sourceInfo.getAddress();
			Logger.network(NetworkLogType.READ, "USERNAME : " + username);
			Logger.network(NetworkLogType.READ, "ADDRESS : " + address);
			ui.displayNewPrivateAcceptionEvent(username.toString());
			launchPrivateConnection(username, address);

			break;
		}

		case PVCOESTADST: {
			DestinationConnection destinationInfo = ClientCommunication.receiveRequestPVCOESTADST(sc);
			Username username = destinationInfo.getUsername();
			InetAddress address = destinationInfo.getAddress();
			int portMessage = destinationInfo.getPortMessage();
			int portFile = destinationInfo.getPortFile();
			Logger.network(NetworkLogType.READ, "USERNAME : " + username);
			Logger.network(NetworkLogType.READ, "ADDRESS : " + address);
			Logger.network(NetworkLogType.READ, "PORT MESSAGE : " + portMessage);
			Logger.network(NetworkLogType.READ, "PORT FILE : " + portFile);
			ui.displayNewPrivateAcceptionEvent(username.toString());
			launchPrivateConnection(username, address, portMessage, portFile);

			break;
		}

		default:
			throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

		}

	}

	/*
	 * Reads message requests from a client.
	 */
	private void privateCommunicationMessage(SocketChannel pv, Username username) throws IOException {
		while (true) {
			Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(pv);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol violation");
			}
			NetworkProtocol protocol = optionalRequestType.get();
			Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

			switch (protocol) {

			case PVMSG: {
				Message message = ClientCommunication.receiveRequestPVMSG(pv, username.toString());
				Logger.network(NetworkLogType.READ, "USERNAME : " + message.getUsername());
				Logger.network(NetworkLogType.READ, "MESSAGE : " + message.getContent());
				ui.displayMessage(message);

				break;
			}

			default:
				throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

			}
		}
	}

	/*
	 * Reads file requests from a client.
	 */
	private void privateCommunicationFile(SocketChannel pv, Username username) throws IOException {
		while (true) {
			Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(pv);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol violation");
			}
			NetworkProtocol protocol = optionalRequestType.get();
			Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

			switch (protocol) {

			case PVFILE: {
				Path path = ClientCommunication.receiveRequestPVFILE(pv, username.toString());
				Logger.network(NetworkLogType.READ, "USERNAME : " + username);
				Logger.network(NetworkLogType.READ, "PATH : " + path);
				ui.displayNewFileReception(username.toString(), path);

				break;
			}

			default:
				throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

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
					Logger.debug("[SOURCE] CONNECTION HACK !!!");
					NetworkCommunication.silentlyClose(pv);
					continue;
				}
				Logger.debug("[SOURCE] CONNECTION ACCEPTED");
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
			registerThread();
			try (SocketChannel scMessage = acceptCommunication(sscMessage, addressDst)) {
				Logger.debug("[SOURCE] MESSAGE CONNECTED");
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
				unregisterThread();
			}
		}, "Private messages : " + username).start();

		new Thread(() -> {
			registerThread();
			try (SocketChannel scFile = acceptCommunication(sscFile, addressDst)) {
				Logger.debug("[SOURCE] FILE CONNECTED");
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
				unregisterThread();
			}
		}, "Private files : " + username).start();

	}

	private void privateCommunicationDestination(Username username, InetAddress addressSrc, int portMessage,
			int portFile) throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		Logger.debug("[DESTINATION] MESSAGE PORT : " + portMessage);
		Logger.debug("[DESTINATION] FILE PORT : " + portFile);

		Logger.debug("[DESTINATION] CONNECTED");

		new Thread(() -> {
			registerThread();
			try {
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateMessageDisconnection(username);
				unregisterThread();
			}
		}, "Private messages : " + username).start();

		new Thread(() -> {
			registerThread();
			try {
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			} finally {
				ui.displayNewPrivateFileDisconnection(username);
				unregisterThread();
			}
		}, "Private files : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressDst) {
		new Thread(() -> {
			registerThread();
			try {
				privateCommunicationSource(username, addressDst);
			} catch (IOException e) {
				Logger.exception(e);
			} finally {
				unregisterThread();
			}
		}, "Private source connection : " + username).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressSrc, int portMessage, int portFile) {
		new Thread(() -> {
			registerThread();
			try {
				privateCommunicationDestination(username, addressSrc, portMessage, portFile);
			} catch (IOException e) {
				Logger.exception(e);
			} finally {
				unregisterThread();
			}
		}, "Private destination connection : " + username).start();
	}

	// FIXME : Déconnexion publique => Déconnexion privée ?

	private void threadMessageSender() {
		registerThread();
		try {
			boolean stop = false;
			while (!Thread.interrupted() && stop == false) {
				try {
					stop = messageSender();
				} catch (IOException e) {
					Logger.warning("WARNING | " + e.toString());
					Logger.exception(e);
					return;
				}
			}
		} finally {
			unregisterThread();
			setExit();
		}
	}

	private void threadMessageReceiver() {
		registerThread();
		try {
			while (!Thread.interrupted()) {
				try {
					messageReceiver();
				} catch (IOException e) {
					Logger.warning("WARNING | " + e.toString());
					Logger.exception(e);
					return;
				}
			}
		} finally {
			unregisterThread();
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

	public void startChat() throws IOException, InterruptedException {
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

	public void startChat(String username) throws IOException, InterruptedException {
		if (!connectUsername(username)) {
			Logger.debug("CONNECTION FAILED");
			return;
		}

		processMessages();

		Logger.debug("DISCONNECTION");
	}

	public void startChat(Optional<String> username) throws IOException, InterruptedException {
		if (username.isPresent()) {
			startChat(username.get());
		} else {
			startChat();
		}
	}

	@Override
	public void close() throws IOException {
		Logger.debug("CLOSING");
		ui.close();
		sc.close();
	}

}
