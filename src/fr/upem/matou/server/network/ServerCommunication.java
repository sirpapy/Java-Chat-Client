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
	static String readStringUTF8(ByteBuffer bb) {
		// TODO : flip + readInt
		return PROTOCOL_CHARSET.decode(bb).toString();
	}

	/*
	 * Adds a CORES request to the buffer.
	 */
	static boolean addRequestCORES(ByteBuffer bbWrite, boolean acceptation) {
		int length = Integer.BYTES + Byte.BYTES;
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.CORES.ordinal());

		if (acceptation) {
			bbWrite.put((byte) 1);
		} else {
			bbWrite.put((byte) 0);
		}

		return true;
	}

	/*
	 * Encodes a MSGBC request.
	 */
	static ByteBuffer encodeRequestMSGBC(String pseudo, String message) {
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
	static ByteBuffer encodeRequestCODISP(String pseudo) {
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
	static ByteBuffer encodeRequestDISCODISP(String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + sizePseudo;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.DISCODISP.ordinal());
		request.putInt(sizePseudo).put(encodedPseudo);

		return request;
	}
}
