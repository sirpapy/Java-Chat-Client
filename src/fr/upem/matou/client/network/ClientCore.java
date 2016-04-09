package fr.upem.matou.client.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
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
public class ClientCore implements Closeable {

	private static final int TIMEOUT = 3000; // in millis

	private final Object monitor = new Object();
	private final SocketChannel sc;
	private final ClientDataBase db;
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

	private void usernameSender(UserInterface ui) throws IOException {
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

	private boolean messageSender(UserInterface ui) throws IOException {
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

	private void messageReceiver(UserInterface ui) throws IOException {
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

		case CODISP: {
			setChrono(true);
			Optional<String> optionalRequestCODISP = ClientCommunication.receiveRequestCODISP(sc);
			setChrono(false);

			if (!optionalRequestCODISP.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedConnected = optionalRequestCODISP.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedConnected);
			ui.displayNewConnectionEvent(receivedConnected);

			break;
		}

		case DISCODISP: {
			setChrono(true);
			Optional<String> optionalRequestDISCODISP = ClientCommunication.receiveRequestDISCODISP(sc);
			setChrono(false);
			if (!optionalRequestDISCODISP.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedDisconnected = optionalRequestDISCODISP.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedDisconnected);
			ui.displayNewDisconnectionEvent(receivedDisconnected);

			break;
		}

		case PVCODISP: {
			setChrono(true);
			Optional<String> optionalRequestPVCODISP = ClientCommunication.receiveRequestPVCODISP(sc);
			setChrono(false);

			if (!optionalRequestPVCODISP.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedConnectionRequest = optionalRequestPVCODISP.get();
			Logger.network(NetworkLogType.READ, "USERNAME : " + receivedConnectionRequest);
			ui.displayNewPrivateRequestEvent(receivedConnectionRequest);

			break;
		}

		default:
			throw new UnsupportedOperationException("Unsupported protocol request : " + protocol); // TEMP

		}

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

	private void threadMessageSender(UserInterface ui) {
		boolean exit = false;
		while (!Thread.interrupted() && exit == false) {
			try {
				exit = messageSender(ui);
			} catch (IOException e) {
				Logger.warning("WARNING | " + e.toString());
				Logger.exception(e);
				return;
			}
		}
	}

	private void threadMessageReceiver(UserInterface ui) {
		while (!Thread.interrupted()) {
			try {
				messageReceiver(ui);
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

	private void processUsername(UserInterface ui) throws IOException {
		boolean isAccepted = false;
		while (!isAccepted) {
			usernameSender(ui);
			isAccepted = usernameReceiver();
		}
	}

	private void processMessages(UserInterface ui) throws InterruptedException, IOException {
		Thread sender = new Thread(() -> threadMessageSender(ui));
		Thread receiver = new Thread(() -> threadMessageReceiver(ui));
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
		UserInterface ui = new ShellInterface();

		processUsername(ui);
		processMessages(ui);

		Logger.debug("DISCONNECTION");
	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

}
