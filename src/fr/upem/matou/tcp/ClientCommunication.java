package fr.upem.matou.tcp;

import static fr.upem.matou.tcp.NetworkCommunication.PROTOCOL_CHARSET;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.ui.Message;

/*
 * This class consists only of static methods.
 * These methods are used by the client to ensure that communications meet the protocol.
 */
class ClientCommunication {

	private ClientCommunication() {
	}

	private static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while (bb.hasRemaining()) {
			int read = sc.read(bb);
			if (read == -1) {
				return false;
			}
		}
		return true;
	}

	private static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	/*
	 * Encodes a COREQ request
	 */
	public static ByteBuffer encodeRequestCOREQ(String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int length = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CLIENT_PUBLIC_CONNECTION_REQUEST.ordinal());
		request.putInt(length).put(encodedPseudo);

		return request;
	}

	/*
	 * Encodes a MSG request
	 */
	public static ByteBuffer encodeRequestMSG(String message) {
		ByteBuffer encodedMessage = PROTOCOL_CHARSET.encode(message);

		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CLIENT_PUBLIC_MESSAGE.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	/*
	 * Sends a COREQ request
	 */
	public static void sendRequestCOREQ(SocketChannel sc, String pseudo) throws IOException {
		ByteBuffer bb = encodeRequestCOREQ(pseudo);
		sendRequest(sc, bb);
	}

	/*
	 * Sends a MSG request
	 */
	public static void sendRequestMSG(SocketChannel sc, String message) throws IOException {
		ByteBuffer bb = encodeRequestMSG(message);
		sendRequest(sc, bb);
	}

	/*
	 * Receives a protocol type request
	 */
	public static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			return Optional.empty();
		}
		bb.flip();
		int ordinal = bb.getInt();
		return NetworkProtocol.getProtocol(ordinal);
	}

	/*
	 * Receives a CORES request
	 */
	public static Optional<Boolean> receiveRequestCORES(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		if (!readFully(sc, bb)) {
			return Optional.empty();
		}
		bb.flip();
		byte acceptation = bb.get();
		return Optional.of(acceptation != 0);
	}

	/*
	 * Receives a MSGBG request
	 */
	public static Optional<Message> receiveRequestMSGBC(SocketChannel sc) throws IOException {
		ByteBuffer bbSizePseudo = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizePseudo)) {
			return Optional.empty();
		}
		bbSizePseudo.flip();
		int sizePseudo = bbSizePseudo.getInt();

		ByteBuffer bbPseudo = ByteBuffer.allocate(sizePseudo);
		if (!readFully(sc, bbPseudo)) {
			return Optional.empty();
		}
		bbPseudo.flip();
		String pseudo = PROTOCOL_CHARSET.decode(bbPseudo).toString();

		ByteBuffer bbSizeMessage = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeMessage)) {
			return Optional.empty();
		}
		bbSizeMessage.flip();
		int sizeMessage = bbSizeMessage.getInt();

		ByteBuffer bbMessage = ByteBuffer.allocate(sizeMessage);
		if (!readFully(sc, bbMessage)) {
			return Optional.empty();
		}
		bbMessage.flip();
		String message = PROTOCOL_CHARSET.decode(bbMessage).toString();

		return Optional.of(new Message(pseudo,message));
	}

}
