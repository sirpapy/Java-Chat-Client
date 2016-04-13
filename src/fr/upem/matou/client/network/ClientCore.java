package fr.upem.matou.client.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;

import fr.upem.matou.client.ui.Message;
import fr.upem.matou.client.ui.ShellInterface;
import fr.upem.matou.client.ui.UserInterface;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

/*
 * This class is the core of the client.
 */
@SuppressWarnings("resource")
public class ClientCore implements Closeable {

	private static final int TIMEOUT = 3000; // in millis

	private final Object monitor = new Object();
	private final SocketChannel sc;
	private final ClientDataBase db;
	private final UserInterface ui = new ShellInterface();
	private boolean isReceiverActivated = false;

	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
		db = new ClientDataBase(sc);
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
			Optional<String> optionalInput = ui.getUsername();
			if (!optionalInput.isPresent()) {
				throw new IOException("User exited"); // TODO : Ne plus renvoyer d'exception
			}
			String username = optionalInput.get();
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
		case CORES:
			Optional<Boolean> optionalRequestCORES = ClientCommunication.receiveRequestCORES(sc);
			if (!optionalRequestCORES.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			boolean acceptation = optionalRequestCORES.get();
			Logger.network(NetworkLogType.READ, "ACCEPTATION : " + acceptation);
			return acceptation;
		default:
			throw new AssertionError("Unexpected protocol request : " + protocol);
		}
	}

	private boolean messageSender() throws IOException {
		Optional<ClientEvent> optionalEvent = ui.getEvent();
		if (!optionalEvent.isPresent()) {
			return true;
		}
		ClientEvent event = optionalEvent.get();

		if (!event.execute(db)) {
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

			Optional<Message> optionalRequestMSGBC = ClientCommunication.receiveRequestMSGBC(sc);
			setChrono(false);

			if (!optionalRequestMSGBC.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			Message receivedMessage = optionalRequestMSGBC.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedMessage.getUsername());
			Logger.network(NetworkLogType.READ, "MESSAGE : " + receivedMessage.getContent());
			ui.displayMessage(receivedMessage);

			break;
		}

		case CONOTIF: {
			setChrono(true);
			Optional<String> optionalRequestCONOTIF = ClientCommunication.receiveRequestCONOTIF(sc);
			setChrono(false);

			if (!optionalRequestCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedConnected = optionalRequestCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedConnected);
			ui.displayNewConnectionEvent(receivedConnected);

			break;
		}

		case DISCONOTIF: {
			setChrono(true);
			Optional<String> optionalRequestDISCONOTIF = ClientCommunication.receiveRequestDISCONOTIF(sc);
			setChrono(false);
			if (!optionalRequestDISCONOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedDisconnected = optionalRequestDISCONOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedDisconnected);
			ui.displayNewDisconnectionEvent(receivedDisconnected);

			break;
		}

		case PVCOREQNOTIF: {
			setChrono(true);
			Optional<String> optionalRequestPVCOREQNOTIF = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);
			setChrono(false);

			if (!optionalRequestPVCOREQNOTIF.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedConnectionRequest = optionalRequestPVCOREQNOTIF.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedConnectionRequest);
			ui.displayNewPrivateRequestEvent(receivedConnectionRequest);

			break;
		}

		case PVCOESTASRC: {
			setChrono(true);
			Optional<SourceConnection> optionalRequestPVCOESTASRC = ClientCommunication.receiveRequestPVCOESTASRC(sc);
			setChrono(false);

			if (!optionalRequestPVCOESTASRC.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			SourceConnection receivedConnectionRequest = optionalRequestPVCOESTASRC.get();
			String username = receivedConnectionRequest.getUsername();
			InetAddress address = receivedConnectionRequest.getAddress();
			Logger.network(NetworkLogType.READ, "USERNAME : " + username);
			Logger.network(NetworkLogType.READ, "ADDRESS : " + address);
			ui.displayNewPrivateAcceptionEvent(username);
			launchPrivateConnection(username, address); // TEMP : variable locale

			break;
		}

		case PVCOESTADST: {
			setChrono(true);
			Optional<DestinationConnection> optionalRequestPVCOESTADST = ClientCommunication
					.receiveRequestPVCOESTADST(sc);
			setChrono(false);

			if (!optionalRequestPVCOESTADST.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			DestinationConnection receivedConnectionRequest = optionalRequestPVCOESTADST.get();
			String username = receivedConnectionRequest.getUsername();
			InetAddress address = receivedConnectionRequest.getAddress();
			int portMessage = receivedConnectionRequest.getPortMessage();
			int portFile = receivedConnectionRequest.getPortFile();
			Logger.network(NetworkLogType.READ, "USERNAME : " + username);
			Logger.network(NetworkLogType.READ, "ADDRESS : " + address);
			Logger.network(NetworkLogType.READ, "PORT MESSAGE : " + portMessage);
			Logger.network(NetworkLogType.READ, "PORT FILE : " + portFile);
			ui.displayNewPrivateAcceptionEvent(username);
			launchPrivateConnection(username, address, portMessage, portFile);

			break;
		}

		default:
			throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

		}

	}

	private void privateCommunicationMessage(SocketChannel pv, String username) throws IOException {
		while (true) {
			Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(pv);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol violation");
			}
			NetworkProtocol protocol = optionalRequestType.get();
			Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

			switch (protocol) {

			case PVMSG: {
				Optional<Message> optionalRequestPVMSG = ClientCommunication.receiveRequestPVMSG(pv, username);

				if (!optionalRequestPVMSG.isPresent()) {
					throw new IOException("Protocol violation : " + protocol);
				}
				Message receivedMessage = optionalRequestPVMSG.get();
				Logger.network(NetworkLogType.READ, "USERNAME : " + receivedMessage.getUsername());
				Logger.network(NetworkLogType.READ, "MESSAGE : " + receivedMessage.getContent());
				ui.displayMessage(receivedMessage);

				break;
			}

			default:
				throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

			}
		}
	}

	private void privateCommunicationFile(SocketChannel pv, String username) throws IOException {
		while (true) {
			Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(pv);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol violation");
			}
			NetworkProtocol protocol = optionalRequestType.get();
			Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);

			switch (protocol) {

			case PVFILE: {
				Optional<Path> optionalRequestPVFILE = ClientCommunication.receiveRequestPVFILE(pv, username);

				if (!optionalRequestPVFILE.isPresent()) {
					throw new IOException("Protocol violation : " + protocol);
				}
				Path path = optionalRequestPVFILE.get();
				Logger.network(NetworkLogType.READ, "USERNAME : " + username);
				Logger.network(NetworkLogType.READ, "PATH : " + path);
				ui.displayFile(username, path);

				break;
			}

			default:
				throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

			}
		}
	}

	private static SocketChannel acceptCommunication(ServerSocketChannel ssc, InetAddress address) throws IOException {
		SocketChannel pv;
		while (true) {
			pv = ssc.accept();
			InetAddress connected = ((InetSocketAddress) pv.getRemoteAddress()).getAddress();
			if (!address.equals(connected)) {
				Logger.debug("CONNECTION HACK !!!");
				// TODO : close
				continue;
			}
			Logger.debug("CONNECTION ACCEPTED");
			return pv;
		}
	}

	private void privateCommunicationSource(String username, InetAddress addressDst) throws IOException {
		ServerSocketChannel sscMessage = ServerSocketChannel.open();
		ServerSocketChannel sscFile = ServerSocketChannel.open();
		sscMessage.bind(null);
		sscFile.bind(null);

		int portMessage = ((InetSocketAddress) sscMessage.getLocalAddress()).getPort();
		int portFile = ((InetSocketAddress) sscFile.getLocalAddress()).getPort();
		System.out.println("MESSAGE PORT : " + portMessage);
		System.out.println("FILE PORT : " + portFile);

		ClientCommunication.sendRequestPVCOPORT(sc, username, portMessage, portFile);

		new Thread(() -> {
			try {
				SocketChannel scMessage = acceptCommunication(sscMessage, addressDst);
				Logger.debug("CONNECTED (SOURCE MESSAGE)");
				db.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();

		new Thread(() -> {
			try {
				SocketChannel scFile = acceptCommunication(sscFile, addressDst);
				Logger.debug("CONNECTED (SOURCE FILE)");
				db.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();
	}

	private void privateCommunicationDestination(String username, InetAddress addressSrc, int portMessage, int portFile)
			throws IOException {
		SocketChannel scMessage = SocketChannel.open(new InetSocketAddress(addressSrc, portMessage));
		SocketChannel scFile = SocketChannel.open(new InetSocketAddress(addressSrc, portFile));

		System.out.println("MESSAGE PORT : " + portMessage);
		System.out.println("FILE PORT : " + portFile);

		Logger.debug("CONNECTED (DESTINATION)");

		new Thread(() -> {
			try {
				db.addNewPrivateMessageChannel(username, scMessage);
				privateCommunicationMessage(scMessage, username);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();

		new Thread(() -> {
			try {
				db.addNewPrivateFileChannel(username, scFile);
				privateCommunicationFile(scFile, username);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();
	}

	private void launchPrivateConnection(String username, InetAddress addressDst) {
		new Thread(() -> {
			try {
				privateCommunicationSource(username, addressDst);
			} catch (IOException e) {
				Logger.exception(e);
			}
		}).start();
	}

	private void launchPrivateConnection(String username, InetAddress addressSrc, int portMessage, int portFile) {
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
		boolean testeur;
		testeur = getChrono();

		if (testeur) {
			long begin = System.currentTimeMillis();
			while (testeur) {
				delay = System.currentTimeMillis() - begin;
				if ((delay) > TIMEOUT) {
					Logger.debug("CLEANER ACTIVATION : delay " + delay + " > " + TIMEOUT);
					setChrono(false);
					try {
						this.sc.close();
						break;
					} catch (IOException e) {
						Logger.exception(e);
					}
				}
				testeur = getChrono();
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
