package fr.upem.matou.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.ui.Message;
import fr.upem.matou.ui.ShellInterface;
import fr.upem.matou.ui.UserInterface;

/*
 * This class is the core of the client.
 */
public class ClientMatou implements Closeable {

	private final SocketChannel sc;

	public ClientMatou(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
	}

	private boolean sender(UserInterface ui) throws IOException {
		Optional<String> optionalInput = ui.readMessage();
		if (!optionalInput.isPresent()) {
			return true;
		}
		String inputMessage = optionalInput.get();
		ClientCommunication.sendRequestMSG(sc, inputMessage);
		return false;
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

	private void receiver(UserInterface ui) throws IOException {
		Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
		if (!optionalRequestType.isPresent()) {
			throw new IOException("Protocol exception");
		}

		NetworkProtocol protocol = optionalRequestType.get();
		System.out.println("PROTOCOL : " + protocol);
		switch (protocol) {
		case SERVER_PUBLIC_MESSAGE_BROADCAST:
			Optional<Message> optionalRequestMSGBC = ClientCommunication.receiveRequestMSGBC(sc);
			if (!optionalRequestMSGBC.isPresent()) {
				throw new IOException("Protocol exception");
			}
			Message receivedMessage = optionalRequestMSGBC.get();
			ui.displayMessage(receivedMessage);
			break;
		default:
			throw new AssertionError("PROTOCOL EXCEPTION");
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
		// TODO : Thread "Cleaner"
	}

	private void processPseudo(UserInterface ui) throws IOException {
		while (true) {
			Optional<String> optionalInput = ui.readPseudo();
			if(!optionalInput.isPresent()) {
				throw new IOException("User exited");
			}
			String pseudo = optionalInput.get();
			ClientCommunication.sendRequestCOREQ(sc, pseudo);

			Optional<NetworkProtocol> optionalRequestType = ClientCommunication.receiveRequestType(sc);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol exception");
			}

			NetworkProtocol protocol = optionalRequestType.get();
			System.out.println("PROTOCOL : " + protocol);
			switch (protocol) {
			case SERVER_PUBLIC_CONNECTION_RESPONSE:
				Optional<Boolean> optionalRequestCORES = ClientCommunication.receiveRequestCORES(sc);
				if (!optionalRequestCORES.isPresent()) {
					throw new IOException("Protocol exception");
				}
				boolean acceptation = optionalRequestCORES.get();
				System.out.println("ACCEPTATION : " + acceptation);
				if (acceptation == true) {
					return;
				}
				break;
			default:
				throw new AssertionError("PROTOCOL EXCEPTION");
			}
		}
	}

	private void processMessages(UserInterface ui) throws InterruptedException {
		Thread sender = new Thread(() -> threadSender(ui));
		Thread receiver = new Thread(() -> threadReceiver(ui));
		sender.start();
		receiver.start();

		sender.join();
		receiver.interrupt();
	}

	public void startChat() throws IOException, InterruptedException {
		UserInterface ui = new ShellInterface();

		processPseudo(ui);
		processMessages(ui);

		System.out.println("DISCONNECTION");
	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

	private static void usage() {
		System.err.println("Usage : host port");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 2) {
			usage();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);

		try (ClientMatou client = new ClientMatou(host, port)) {
			client.startChat();
		}
	}

}
