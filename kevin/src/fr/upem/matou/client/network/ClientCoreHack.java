package fr.upem.matou.client.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

// TEMP : Crash Test

/*
 * This class is the core of the client.
 */
public class ClientCoreHack implements Closeable {

	private final SocketChannel sc;

	public ClientCoreHack(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
	}

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();

	public static ByteBuffer encodeRequestMSGBC(String pseudo, String message) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);
		ByteBuffer encodedMessage = PROTOCOL_CHARSET.encode(message);

		int sizePseudo = encodedPseudo.remaining();
		int sizeMessage = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + sizePseudo + Integer.BYTES + sizeMessage;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.MSGBC.ordinal());
		request.putInt(sizePseudo).put(encodedPseudo);
		request.putInt(sizeMessage).put(encodedMessage);

		return request;
	}

	private static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	public static void sendRequestMSGBC(SocketChannel sc, String pseudo, String message) throws IOException {
		ByteBuffer bb = encodeRequestMSGBC(pseudo, message);
		sendRequest(sc, bb);
	}

	// OK
	public void startChat_ServerReservedRequest() throws IOException {
		sendRequestMSGBC(sc, "foo", "abra kadabra");
		Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
		if (!optionalProtocol.isPresent()) {
			System.out.println("No protocol");
			return;
		}
		NetworkProtocol protocol = optionalProtocol.get();

		switch (protocol) {
		case MSGBC:
			Message message = ClientCommunication.receiveRequestMSGBC(sc);
			System.out.println(message);
			break;
		default:
			System.out.println("Invalid protocol : " + protocol);
			break;
		}
	}

	// OK
	public void startChat_MultipleCOREQ() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "abra");
		ClientCommunication.sendRequestMSG(sc, "abra");
		ClientCommunication.sendRequestCOREQ(sc, "kadabra");
		ClientCommunication.sendRequestMSG(sc, "kadabra");

		for (int i = 0; i < 6; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}
	}

	// OK
	public void startChat_PseudoEmpty() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "");

		for (int i = 0; i < 2; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}

	}

	// OK
	public void startChat_PseudoBigger() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "abraabraabraabraabraabraabraabra!");

		for (int i = 0; i < 2; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}

	}

	// OK
	public void startChat_MessageBigger() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "abra");
		ClientCommunication.sendRequestMSG(sc,
				"abraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabraabra!");

		for (int i = 0; i < 3; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}

	}

	// OK
	public void startChat_MessageEmpty() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "foo");
		ClientCommunication.sendRequestMSG(sc, "");

		for (int i = 0; i < 3; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}

	}

	// OK
	public void startChat_FullMessage() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "øøøøøøøøøøøøøøøø");
		ClientCommunication.sendRequestMSG(sc,
				"𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀");
		for (int i = 0; i < 3; i++) {
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}
	}

	// OK
	public void startChat_FloodMessage() throws IOException {
		ClientCommunication.sendRequestCOREQ(sc, "øøøøøøøøøøøøøøøø");
		int flood = 32;
		for (int i = 0; i < flood; i++) {
			ClientCommunication.sendRequestMSG(sc,
					"𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀");
		}

		int stop = 2 + flood;
		for (int i = 0; i < stop; i++) {
			System.out.println("REQ #" + (i + 1));
			Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
			if (!optionalProtocol.isPresent()) {
				System.out.println("No protocol");
				return;
			}
			NetworkProtocol protocol = optionalProtocol.get();

			switch (protocol) {
			case CORES:
				boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
				System.out.println("Acceptation = " + acceptation);
				break;
			case CONOTIF:
				String pseudo = ClientCommunication.receiveRequestCONOTIF(sc);
				System.out.println("New connection : " + pseudo);
				break;
			case MSGBC:
				Message message = ClientCommunication.receiveRequestMSGBC(sc);
				System.out.println(message);
				break;
			default:
				System.out.println("Invalid protocol : " + protocol);
				return;
			}
		}

		System.out.println("SUCCESS");
	}

	// OK
	public void startChat_UnauthentMessage() throws IOException {
		ClientCommunication.sendRequestMSG(sc, "Hello world");

		Optional<NetworkProtocol> optionalProtocol = ClientCommunication.receiveRequestType(sc);
		if (!optionalProtocol.isPresent()) {
			System.out.println("No protocol");
			return;
		}
		NetworkProtocol protocol = optionalProtocol.get();

		switch (protocol) {
		case MSGBC:
			Message message = ClientCommunication.receiveRequestMSGBC(sc);
			System.out.println(message);
			break;
		default:
			System.out.println("Invalid protocol : " + protocol);
			return;
		}

	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

	public void startChat() throws IOException {
		startChat_UnauthentMessage();
	}

}
