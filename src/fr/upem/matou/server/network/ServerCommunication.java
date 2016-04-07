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
	static boolean addRequestMSGBC(ByteBuffer bbWrite, String pseudo, String message) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);
		ByteBuffer encodedMessage = PROTOCOL_CHARSET.encode(message);

		int sizePseudo = encodedPseudo.remaining();
		int sizeMessage = encodedMessage.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizePseudo + Integer.BYTES + sizeMessage;
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.MSGBC.ordinal());
		bbWrite.putInt(sizePseudo).put(encodedPseudo);
		bbWrite.putInt(sizeMessage).put(encodedMessage);

		return true;
	}

	/*
	 * Encodes a CODISP request.
	 */
	static boolean addRequestCODISP(ByteBuffer bbWrite, String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizePseudo;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.CODISP.ordinal());
		bbWrite.putInt(sizePseudo).put(encodedPseudo);

		return true;
	}

	/*
	 * Encodes a DISCODISP request.
	 */
	static boolean addRequestDISCODISP(ByteBuffer bbWrite, String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizePseudo;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.DISCODISP.ordinal());
		bbWrite.putInt(sizePseudo).put(encodedPseudo);

		return true;
	}

	static boolean addRequestPVCODISP(ByteBuffer bbWrite, String pseudo) {
		ByteBuffer encodedPseudo = PROTOCOL_CHARSET.encode(pseudo);

		int sizePseudo = encodedPseudo.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizePseudo;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.PVCODISP.ordinal());
		bbWrite.putInt(sizePseudo).put(encodedPseudo);

		return true;
	}
}
