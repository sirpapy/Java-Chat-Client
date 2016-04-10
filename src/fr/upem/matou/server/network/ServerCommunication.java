package fr.upem.matou.server.network;

import java.net.InetAddress;
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
	 * Encodes a CONOTIF request.
	 */
	static boolean addRequestCONOTIF(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.CONOTIF.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}

	/*
	 * Encodes a DISCONOTIF request.
	 */
	static boolean addRequestDISCONOTIF(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.DISCONOTIF.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}

	static boolean addRequestPVCOREQNOTIF(ByteBuffer bbWrite, String username) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.PVCOREQNOTIF.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		return true;
	}

	static boolean addRequestPVCOESTASRC(ByteBuffer bbWrite, String username, InetAddress address) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);

		int sizeUsername = encodedUsername.remaining();

		int length = Integer.BYTES + Integer.BYTES + sizeUsername;
		if (bbWrite.remaining() < length) {
			return false;
		}
		bbWrite.putInt(NetworkProtocol.PVCOESTASRC.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);

		byte[] addr = address.getAddress();
		bbWrite.putInt(addr.length);
		
		for(byte b : addr) {
			bbWrite.put(b);
		}

		return true;
	}
}
