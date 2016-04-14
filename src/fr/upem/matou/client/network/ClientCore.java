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

	private static final int TIMEOUT = 3000; // in millis

	private final Object monitor = new Object();
	private final SocketChannel sc;
	private final ClientSession session;
	private final UserInterface ui = new ShellInterface();
	private boolean isReceiverActivated = false;

	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
		session = new ClientSession(sc);
	}

	private void setChrono(boolean isActivated) {
		synchronized (monitor) {
			isReceiverActivated = isActivated;
		}
	}

	private boolean getChrono() {
		synchronized (monitor) {
			return isReceiverActivated;
		}
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

	private void messageReceiver() throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol violation");
		}
		NetworkProtocol protocol = optionalRequestType.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

		switch (protocol) {

		case MSGBC: {
			setChrono(true);
			Optional<Message> optionalMSGBC = ClientCommunication.receiveRequestMSGBC(sc);
			setChrono(false);

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
			setChrono(true);
			Optional<String> optionalCONOTIF = ClientCommunication.receiveRequestCONOTIF(sc);
			setChrono(false);

			if (!optionalCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameConnected = optionalCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameConnected);
			ui.displayNewConnectionEvent(usernameConnected);

			break;
		}

		case DISCONOTIF: {
			setChrono(true);
			Optional<String> optionalDISCONOTIF = ClientCommunication.receiveRequestDISCONOTIF(sc);
			setChrono(false);

			if (!optionalDISCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameDisconnected = optionalDISCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameDisconnected);
			ui.displayNewDisconnectionEvent(usernameDisconnected);

			break;
		}

		case PVCOREQNOTIF: {
			setChrono(true);
			Optional<String> optionalPVCOREQNOTIF = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);
			setChrono(false);

			if (!optionalPVCOREQNOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String usernameRequester = optionalPVCOREQNOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + usernameRequester);
			ui.displayNewPrivateRequestEvent(usernameRequester);

			break;
		}

		case PVCOESTASRC: {
			setChrono(true);
			Optional<SourceConnection> optionalPVCOESTASRC = ClientCommunication.receiveRequestPVCOESTASRC(sc);
			setChrono(false);

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
			setChrono(true);
			Optional<DestinationConnection> optionalPVCOESTADST = ClientCommunication.receiveRequestPVCOESTADST(sc);
			setChrono(false);

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
				ui.displayFile(username.toString(), path);

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
					Logger.debug("CONNECTION HACK !!!");
					pv.close();
					continue;
				}
				Logger.debug("CONNECTION ACCEPTED");
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
		System.out.println("MESSAGE PORT : " + portMessage);
		System.out.println("FILE PORT : " + portFile);

		ClientCommunication.sendRequestPVCOPORT(sc, username.toString(), portMessage, portFile);

		new Thread(() -> {
			try (SocketChannel scMessage = acceptCommunication(sscMessage, addressDst)) {
				Logger.debug("CONNECTED (SOURCE MESSAGE)");
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
		}).start();

		new Thread(() -> {
			try (SocketChannel scFile = acceptCommunication(sscFile, addressDst)) {
				Logger.debug("CONNECTED (SOURCE FILE)");
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
		}).start();

	}

	private void privateCommunicationDestination(Username username, InetAddress addressSrc, int portMessage,
			int portFile) throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		System.out.println("MESSAGE PORT : " + portMessage);
		System.out.println("FILE PORT : " + portFile);

		Logger.debug("CONNECTED (DESTINATION)");

		new Thread(() -> {
			try {
				session.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
		}).start();

		new Thread(() -> {
			try {
				session.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
				session.closePrivateConnection(username);
			}
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

	private void cleaner() throws InterruptedException {
		long delay = 0;
		boolean isReceiving = getChrono();

		if (isReceiving) {
			long begin = System.currentTimeMillis();
			while (isReceiving) {
				delay = System.currentTimeMillis() - begin;
				if ((delay) > TIMEOUT) {
					Logger.warning("CLEANER ACTIVATION : delay " + delay + " > " + TIMEOUT);
					setChrono(false);
					try {
						sc.close();
						break;
					} catch (IOException e) {
						Logger.exception(e);
						return;
					}
				}
				isReceiving = getChrono();
			}

		} else {
			Thread.sleep(TIMEOUT);
		}

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

	private void threadCleaner() {
		while (!Thread.interrupted()) {
			try {
				cleaner();
			} catch (InterruptedException e) {
				Logger.warning("INTERRUPTION | " + e.toString());
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

	private void processMessages() throws InterruptedException, IOException {
		Thread sender = new Thread(() -> threadMessageSender());
		Thread receiver = new Thread(() -> threadMessageReceiver());
		Thread cleaner = new Thread(() -> threadCleaner());

		sender.start();
		receiver.start();
		cleaner.start();

		sender.join();
		warnDisconnection();

		receiver.interrupt();
		cleaner.interrupt();
	}

	private void warnDisconnection() throws IOException {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.DISCO);
		ClientCommunication.sendRequestDISCO(sc);
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
