package fr.upem.matou.server.network;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

/*
 * This class consists only of static methods.
 * These methods are used by the server to ensure that communications meet the protocol.
 */
class ServerCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();

	private ServerCommunication() {
	}

	// bb should be flipped
	public static String readStringUTF8(ByteBuffer bb) {
		return PROTOCOL_CHARSET.decode(bb).toString();
	}

	/*
	 * Encodes a CORES request.
	 */
	public static ByteBuffer encodeRequestCORES(boolean acceptation) {
		int capacity = Integer.BYTES + Byte.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CORES.ordinal());

		if (acceptation) {
			request.put((byte) 1);
		} else {
			request.put((byte) 0);
		}

		return request;
	}

	/*
	 * Encodes a MSGBC request.
	 */
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

	/*
	 * Encodes a CODISP request.
	 */
	public static ByteBuffer encodeRequestCODISP(String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + sizePseudo;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.CODISP.ordinal());
		request.putInt(sizePseudo).put(encodedPseudo);

		return request;
	}

	/*
	 * Encodes a DISCODISP request.
	 */
	public static ByteBuffer encodeRequestDISCODISP(String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + sizePseudo;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.DISCODISP.ordinal());
		request.putInt(sizePseudo).put(encodedPseudo);

		return request;
	}
}
