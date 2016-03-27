package fr.upem.matou.nonblocking.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.ui.ShellInterface;
import fr.upem.matou.ui.UserInterface;

public class ClientMatou implements Closeable {

	private final SocketChannel sc;

	public ClientMatou(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
	}

	private void processPseudo(UserInterface ui) throws IOException {
		while (true) {
			String pseudo = ui.readPseudo();
			NetworkClientCommunication.sendRequestCOREQ(sc, pseudo);

			Optional<NetworkProtocol> optionalRequestType = NetworkClientCommunication.receiveRequestType(sc);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol exception");
			}

			NetworkProtocol protocol = optionalRequestType.get();
			System.out.println("PROTOCOL : " + protocol);
			switch (protocol) {
			case SERVER_PUBLIC_CONNECTION_RESPONSE:
				Optional<Boolean> optionalRequestCORES = NetworkClientCommunication.receiveRequestCORES(sc);
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
			String message = ui.readMessage();
			NetworkClientCommunication.sendRequestMSG(sc, message);

			Optional<NetworkProtocol> optionalRequestType = NetworkClientCommunication.receiveRequestType(sc);
			if (!optionalRequestType.isPresent()) {
				throw new IOException("Protocol exception");
			}

			NetworkProtocol protocol = optionalRequestType.get();
			System.out.println("PROTOCOL : " + protocol);
			switch (protocol) {
			case SERVER_PUBLIC_MESSAGE_BROADCAST:
				Optional<String> optionalRequestMSGBC = NetworkClientCommunication.receiveRequestMSGBC(sc);
				if (!optionalRequestMSGBC.isPresent()) {
					throw new IOException("Protocol exception");
				}
				String received = optionalRequestMSGBC.get();
				System.out.println("MESSAGE : " + received);
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
