package fr.upem.matou.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.logger.Logger;
import fr.upem.matou.logger.Logger.LogType;
import fr.upem.matou.ui.Message;
import fr.upem.matou.ui.ShellInterface;
import fr.upem.matou.ui.UserInterface;

/*
 * This class is the core of the client.
 */
public class ClientCore implements Closeable {

	private final SocketChannel sc;
	private final int TIMEOUT = 1000;
	private boolean isReceiverActivated = false;
	private static final Object monitor = new Object();

	public ClientCore(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
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

	private boolean sender(UserInterface ui) throws IOException {
		Optional<String> optionalInput = ui.readMessage();
		if (!optionalInput.isPresent()) {
			return true;
		}
		String inputMessage = optionalInput.get();
		Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSG);
		Logger.network(LogType.WRITE, "MESSAGE : " + inputMessage);
		ClientCommunication.sendRequestMSG(sc, inputMessage);
		return false;
	}

	private void receiver(UserInterface ui) throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication
				.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol violation");
		}
		NetworkProtocol protocol = optionalRequestType.get();
		Logger.network(LogType.READ, "PROTOCOL : " + protocol);

		switch (protocol) {
		case MSGBC:
			setChrono(true);

			Optional<Message> optionalRequestMSGBC = ClientCommunication
					.receiveRequestMSGBC(sc);
			setChrono(false);

			if (!optionalRequestMSGBC.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			Message receivedMessage = optionalRequestMSGBC.get();
			Logger.network(LogType.READ,
					"PSEUDO : " + receivedMessage.getPseudo());
			Logger.network(LogType.READ,
					"MESSAGE : " + receivedMessage.getContent());
			ui.displayMessage(receivedMessage);

			break;
		case CODISP:
			setChrono(true);
			Optional<String> optionalRequestCODISP = ClientCommunication
					.receiveRequestCODISP(sc);
			setChrono(false);

			if (!optionalRequestCODISP.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedConnected = optionalRequestCODISP.get();
			Logger.network(LogType.READ, "PSEUDO : " + receivedConnected);
			ui.displayNewConnectionEvent(receivedConnected);

			break;
		case DISCODISP:
			setChrono(true);
			Optional<String> optionalRequestDISCODISP = ClientCommunication
					.receiveRequestDISCODISP(sc);
			setChrono(false);
			if (!optionalRequestDISCODISP.isPresent()) {
				throw new IOException("Protocol violation : " + protocol);
			}
			String receivedDisconnected = optionalRequestDISCODISP.get();
			Logger.network(LogType.READ, "PSEUDO : " + receivedDisconnected);
			ui.displayNewDisconnectionEvent(receivedDisconnected);

			break;
		default:
			throw new AssertionError("Unexpected protocol request : "
					+ protocol);
		}

	}

	private void cleaner() throws InterruptedException {
		long delay = 0;
		boolean testeur;
		testeur = getChrono();

		if (testeur) {
			long begin = System.currentTimeMillis();
			while (testeur) {
				Thread.sleep(100);
					delay = System.currentTimeMillis() - begin;
				if ((delay) >= TIMEOUT) {
					try {
						this.sc.close();
						setChrono(false);
						break;
					} catch (IOException e) {
					}
				}

				testeur = getChrono();

			}

		}else{
			Thread.sleep(100);
		}

		/*
		 * TODO (Pape) :
		 * 
		 * Cette méthode doit vérifier le contrat suivant : - si le
		 * receiverThread bloque depuis strictement plus de TIMEOUT
		 * millisecondes alors la SocketChannel associée à ce thread est fermée
		 * 
		 * Etape 1 : Receiver bloque sur une lecture TCP Etape 2 : Cleaner
		 * déclenche un chrono Etape 3 : Si jamais le compte à rebours du chrono
		 * s'arrête AVANT que receiver ait terminée sa lecture : => Cleaner
		 * interrompt Receiver
		 */
	}

	private void threadSender(UserInterface ui) {
		boolean exit = false;
		while (!Thread.interrupted() && exit == false) {
			try {
				exit = sender(ui);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private void threadReceiver(UserInterface ui) {
		while (!Thread.interrupted()) {
			try {
				receiver(ui);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private void threadCleaner() {
		while (!Thread.interrupted()) {
			try {
				cleaner();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private void processPseudo(UserInterface ui) throws IOException {
		while (true) {
			Optional<String> optionalInput = ui.readPseudo();
			if (!optionalInput.isPresent()) {
				throw new IOException("User exited");
			}
			String pseudo = optionalInput.get();
			Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.COREQ);
			Logger.network(LogType.WRITE, "PSEUDO : " + pseudo);
			ClientCommunication.sendRequestCOREQ(sc, pseudo);

			Optional<NetworkProtocol> optionalRequestType = ClientCommunication
					.receiveRequestType(sc);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol violation");
			}

			NetworkProtocol protocol = optionalRequestType.get();
			Logger.network(LogType.READ, "PROTOCOL : " + protocol);
			switch (protocol) {
			case CORES:
				Optional<Boolean> optionalRequestCORES = ClientCommunication
						.receiveRequestCORES(sc);
				if (!optionalRequestCORES.isPresent()) {
					throw new IOException("Protocol violation : " + protocol);
				}
				boolean acceptation = optionalRequestCORES.get();
				Logger.network(LogType.READ, "ACCEPTATION : " + acceptation);
				if (acceptation == true) {
					return;
				}
				break;
			default:
				throw new AssertionError("Unexpected protocol request : "
						+ protocol);
			}
		}
	}

	private void processMessages(UserInterface ui) throws InterruptedException,
			IOException {
		Thread sender = new Thread(() -> threadSender(ui));
		Thread receiver = new Thread(() -> threadReceiver(ui));
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
		Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.DISCO);
		ClientCommunication.sendRequestDISCO(sc);
	}

	public void startChat() throws IOException, InterruptedException {
		UserInterface ui = new ShellInterface();

		processPseudo(ui);
		processMessages(ui);

		Logger.debug("DISCONNECTION");
	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

}
