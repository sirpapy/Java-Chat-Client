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
	
	private void threadSender() {
		// TODO : Thread "Sender"
	}
	
	private void threadReceiver() {
		// TODO : Thread "Receiver"
	}
	
	private void threadCleaner() {
		// TODO : Thread "Cleaner"
	}

	private void processPseudo(UserInterface ui) throws IOException {
		while (true) {
			String pseudo = ui.readPseudo();
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

	private void processMessages(UserInterface ui) throws IOException {
		while (!Thread.interrupted()) {
			String inputMessage = ui.readMessage();
			ClientCommunication.sendRequestMSG(sc, inputMessage);

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
	}

	public void startChat() throws IOException {
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

	public static void main(String[] args) throws IOException {
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
