package fr.upem.matou.nonblocking.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

public class NetworkClientCommunication {
	
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	// TODO : Thread "Inactive Cleaning"

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
	
	public static ByteBuffer encodeRequestCOREQ(String pseudo) {
		ByteBuffer encodedPseudo = UTF8_CHARSET.encode(pseudo);

		int length = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CLIENT_PUBLIC_CONNECTION_REQUEST.ordinal());
		request.putInt(length).put(encodedPseudo);

		return request;
	}
	
	public static ByteBuffer encodeRequestMSG(String message) {
		ByteBuffer encodedMessage = UTF8_CHARSET.encode(message);

		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CLIENT_PUBLIC_MESSAGE.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	public static void sendRequestCOREQ(SocketChannel sc, String pseudo) throws IOException {
		ByteBuffer bb = encodeRequestCOREQ(pseudo);
		sendRequest(sc, bb);
	}

	public static void sendRequestMSG(SocketChannel sc, String message) throws IOException {
		ByteBuffer bb = encodeRequestMSG(message);
		sendRequest(sc, bb);
	}

	public static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			return Optional.empty();
		}
		bb.flip();
		int ordinal = bb.getInt();
		return NetworkProtocol.getProtocol(ordinal);
	}

	public static Optional<Boolean> receiveRequestCORES(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		if (!readFully(sc, bb)) {
			return Optional.empty();
		}
		bb.flip();
		byte acceptation = bb.get();
		return Optional.of(acceptation != 0);
	}

	public static Optional<String> receiveRequestMSGBC(SocketChannel sc) throws IOException {
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
		String pseudo = UTF8_CHARSET.decode(bbPseudo).toString();
		
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
		String message = UTF8_CHARSET.decode(bbMessage).toString();
		
		return Optional.of("<" + pseudo + "> " + message);
	}

}
