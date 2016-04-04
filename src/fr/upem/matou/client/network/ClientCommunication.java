package fr.upem.matou.client.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.ui.Message;

/*
 * This class consists only of static methods.
 * These methods are used by the client to ensure that communications meet the protocol.
 */
class ClientCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();

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

	public static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	/*
	 * Encodes a COREQ request.
	 */
	public static ByteBuffer encodeRequestCOREQ(ByteBuffer encodedPseudo) {
		int length = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.COREQ.ordinal());
		request.putInt(length).put(encodedPseudo);

		return request;
	}

	/*
	 * Encodes a MSG request.
	 */
	public static ByteBuffer encodeRequestMSG(ByteBuffer encodedMessage) {
		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.MSG.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	public static ByteBuffer encodeRequestDISCO() {
		int capacity = Integer.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.DISCO.ordinal());

		return request;
	}

	/*
	 * Sends a COREQ request.
	 */
	public static boolean sendRequestCOREQ(SocketChannel sc, String pseudo) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodePseudo(pseudo);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestCOREQ(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	/*
	 * Sends a MSG request.
	 */
	public static boolean sendRequestMSG(SocketChannel sc, String message) throws IOException {
		if(!NetworkCommunication.checkMessageValidity(message)) {
			return false;
		}
		
		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}
		
		ByteBuffer bb = encodeRequestMSG(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	public static void sendRequestDISCO(SocketChannel sc) throws IOException {
		ByteBuffer bb = encodeRequestDISCO();
		sendRequest(sc, bb);
	}

	/*
	 * Receives a protocol type request.
	 */
	public static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		int ordinal = bb.getInt();
		return NetworkProtocol.getProtocol(ordinal);
	}

	/*
	 * Receives a CORES request.
	 */
	public static Optional<Boolean> receiveRequestCORES(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		byte acceptation = bb.get();
		return Optional.of(acceptation != 0);
	}

	/*
	 * Receives a MSGBG request.
	 */
	public static Optional<Message> receiveRequestMSGBC(SocketChannel sc) throws IOException {
		ByteBuffer bbSizePseudo = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizePseudo)) {
			throw new IOException("Connection closed");
		}
		bbSizePseudo.flip();
		int sizePseudo = bbSizePseudo.getInt();

		ByteBuffer bbPseudo = ByteBuffer.allocate(sizePseudo);
		if (!readFully(sc, bbPseudo)) {
			throw new IOException("Connection closed");
		}
		bbPseudo.flip();
		String pseudo = PROTOCOL_CHARSET.decode(bbPseudo).toString();

		ByteBuffer bbSizeMessage = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeMessage)) {
			throw new IOException("Connection closed");
		}
		bbSizeMessage.flip();
		int sizeMessage = bbSizeMessage.getInt();

		ByteBuffer bbMessage = ByteBuffer.allocate(sizeMessage);
		if (!readFully(sc, bbMessage)) {
			throw new IOException("Connection closed");
		}
		bbMessage.flip();
		String message = PROTOCOL_CHARSET.decode(bbMessage).toString();

		return Optional.of(new Message(pseudo, message));
	}

	/*
	 * Receives a CODISP request.
	 */
	public static Optional<String> receiveRequestCODISP(SocketChannel sc) throws IOException {
		ByteBuffer bbSizePseudo = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizePseudo)) {
			throw new IOException("Connection closed");
		}
		bbSizePseudo.flip();
		int sizePseudo = bbSizePseudo.getInt();

		ByteBuffer bbPseudo = ByteBuffer.allocate(sizePseudo);
		if (!readFully(sc, bbPseudo)) {
			throw new IOException("Connection closed");
		}
		bbPseudo.flip();
		String pseudo = PROTOCOL_CHARSET.decode(bbPseudo).toString();

		return Optional.of(pseudo);
	}

	/*
	 * Receives a DISCODISP request.
	 */
	public static Optional<String> receiveRequestDISCODISP(SocketChannel sc) throws IOException {
		ByteBuffer bbSizePseudo = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizePseudo)) {
			throw new IOException("Connection closed");
		}
		bbSizePseudo.flip();
		int sizePseudo = bbSizePseudo.getInt();

		ByteBuffer bbPseudo = ByteBuffer.allocate(sizePseudo);
		if (!readFully(sc, bbPseudo)) {
			throw new IOException("Connection closed");
		}
		bbPseudo.flip();
		String pseudo = PROTOCOL_CHARSET.decode(bbPseudo).toString();

		return Optional.of(pseudo);
	}

}
