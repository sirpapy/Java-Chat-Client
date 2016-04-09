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
	static boolean addRequestMSGBC(ByteBuffer bbWrite, String username, String message) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);
		ByteBuffer encodedMessage = PROTOCOL_CHARSET.encode(message);

		int sizeUsername = encodedUsername.remaining();
		int sizeMessage = encodedMessage.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername + Integer.BYTES + sizeMessage;
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.MSGBC.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);
		bbWrite.putInt(sizeMessage).put(encodedMessage);

		return true;
	}

	/*
	 * Encodes a CODISP request.
	 */
	static boolean addRequestCODISP(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.CODISP.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}

	/*
	 * Encodes a DISCODISP request.
	 */
	static boolean addRequestDISCODISP(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.DISCODISP.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}

	static boolean addRequestPVCODISP(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.PVCODISP.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}
}
