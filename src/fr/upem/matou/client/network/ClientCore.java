package fr.upem.matou.client.network;

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
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class is the core of the client.
 */
@SuppressWarnings("resource")
public class ClientCore implements Closeable {

	private final SocketChannel sc;
	private final ClientSession session;
	private final UserInterface ui = new ShellInterface();

	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
		session = new ClientSession(sc);
	}

	private void usernameSender() throws IOException {
		while (true) {
			Optional<String> optional = ui.getUsername();
			if (!optional.isPresent()) {
				throw new IOException("User exited"); // TEMP
			}
			String username = optional.get();
			if (!NetworkCommunication.checkUsernameValidity(username)) {
				ui.warnInvalidUsername(username);
				continue;
			}
			Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.COREQ);
			Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
			if (!ClientCommunication.sendRequestCOREQ(sc, username)) {
				ui.warnInvalidUsername(username);
				continue;
			}
			return;
		}
	}

	private boolean usernameReceiver() throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol violation");
		}

		NetworkProtocol protocol = optionalRequestType.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);
		switch (protocol) {
		case CORES: {
			Optional<Boolean> optionalCORES = ClientCommunication.receiveRequestCORES(sc);
			if (!optionalCORES.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			boolean acceptation = optionalCORES.get();
			Logger.network(NetworkLogType.READ, "ACCEPTATION : " + acceptation);
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

		case MSGBC: {
			Optional<Message> optionalMSGBC = ClientCommunication.receiveRequestMSGBC(sc);

			if (!optionalMSGBC.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			Message message = optionalMSGBC.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + message.getUsername());
			Logger.network(NetworkLogType.READ, "MESSAGE : " + message.getContent());
			ui.displayMessage(message);

			break;
		}

		case CONOTIF: {
			Optional<String> optionalCONOTIF = ClientCommunication.receiveRequestCONOTIF(sc);

			if (!optionalCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameConnected = optionalCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameConnected);
			ui.displayNewConnectionEvent(usernameConnected);

			break;
		}

		case DISCONOTIF: {
			Optional<String> optionalDISCONOTIF = ClientCommunication.receiveRequestDISCONOTIF(sc);

			if (!optionalDISCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameDisconnected = optionalDISCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameDisconnected);
			ui.displayNewDisconnectionEvent(usernameDisconnected);

			break;
		}

		case PVCOREQNOTIF: {
			Optional<String> optionalPVCOREQNOTIF = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);

			if (!optionalPVCOREQNOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameRequester = optionalPVCOREQNOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameRequester);
			ui.displayNewPrivateRequestEvent(usernameRequester);

			break;
		}

		case PVCOESTASRC: {
			Optional<SourceConnection> optionalPVCOESTASRC = ClientCommunication.receiveRequestPVCOESTASRC(sc);

			if (!optionalPVCOESTASRC.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			SourceConnection sourceInfo = optionalPVCOESTASRC.get();
			Username username = sourceInfo.getUsername();
			InetAddress address = sourceInfo.getAddress();
			Logger.network(NetworkLogType.READ, "USERNAME : " + username);
			Logger.network(NetworkLogType.READ, "ADDRESS : " + address);
			ui.displayNewPrivateAcceptionEvent(username.toString());
			launchPrivateConnection(username, address);

			break;
		}

		case PVCOESTADST: {
			Optional<DestinationConnection> optionalPVCOESTADST = ClientCommunication.receiveRequestPVCOESTADST(sc);

			if (!optionalPVCOESTADST.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			DestinationConnection destinationInfo = optionalPVCOESTADST.get();
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
				Optional<Message> optionalPVMSG = ClientCommunication.receiveRequestPVMSG(pv,
						username.toString());

				if (!optionalPVMSG.isPresent()) {
					throw new IOException("Protocol violation : " + protocol);
				}
				Message message = optionalPVMSG.get();
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
				Optional<Path> optionalPVFILE = ClientCommunication.receiveRequestPVFILE(pv, username.toString());

				if (!optionalPVFILE.isPresent()) {
					throw new IOException("Protocol violation : " + protocol);
				}
				Path path = optionalPVFILE.get();
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
			try (SocketChannel scMessage = acceptCommunication(sscMessage, addressDst)) {
				Logger.debug("[SOURCE] MESSAGE CONNECTED");
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
			ui.displayNewPrivateMessageDisconnection(username);
		}).start();

		new Thread(() -> {
			try (SocketChannel scFile = acceptCommunication(sscFile, addressDst)) {
				Logger.debug("[SOURCE] FILE CONNECTED");
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
			ui.displayNewPrivateFileDisconnection(username);
		}).start();

	}

	private void privateCommunicationDestination(Username username, InetAddress addressSrc, int portMessage,
			int portFile) throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		Logger.debug("[DESTINATION] MESSAGE PORT : " + portMessage);
		Logger.debug("[DESTINATION] FILE PORT : " + portFile);

		Logger.debug("[DESTINATION] CONNECTED");

		new Thread(() -> {
			try {
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
			ui.displayNewPrivateMessageDisconnection(username);
		}).start();

		new Thread(() -> {
			try {
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
			ui.displayNewPrivateFileDisconnection(username);
		}).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressDst) {
		new Thread(() -> {
			try {
				privateCommunicationSource(username, addressDst);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();
	}

	private void launchPrivateConnection(Username username, InetAddress addressSrc, int portMessage, int portFile) {
		new Thread(() -> {
			try {
				privateCommunicationDestination(username, addressSrc, portMessage, portFile);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();
	}

	private void threadMessageSender() {
		boolean exit = false;
		while (!Thread.interrupted() && exit == false) {
			try {
				exit = messageSender();
			} catch (IOException e) {
				Logger.warning("WARNING | " + e.toString());
				Logger.exception(e);
				return;
			}
		}
	}

	private void threadMessageReceiver() {
		while (!Thread.interrupted()) {
			try {
				messageReceiver();
			} catch (IOException e) {
				Logger.warning("WARNING | " + e.toString());
				Logger.exception(e);
				return;
			}
		}
	}

	private void processUsername() throws IOException {
		boolean isAccepted = false;
		while (!isAccepted) {
			usernameSender();
			isAccepted = usernameReceiver();
		}
	}

	private void processMessages() throws InterruptedException {
		Thread sender = new Thread(() -> threadMessageSender());
		Thread receiver = new Thread(() -> threadMessageReceiver());

		sender.start();
		receiver.start();

		sender.join();

		receiver.interrupt();
	}

	public void startChat() throws IOException, InterruptedException {
		processUsername();
		processMessages();
		Logger.debug("DISCONNECTION");
	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

}
