package fr.upem.matou.server.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.NetworkProtocol.Communicator;

/*
 * This class consists only of static methods. These methods are used by the server to ensure that communications meet
 * the protocol.
 * 
 * All "addRequest" methods append data to a given ByteBuffer in write mode. If the buffer was successfully modified,
 * then true is returned. But if the given buffer does not have enough space to hold all the expected request, then the
 * buffer is not modified and false is returned.
 */
class ServerCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();
	private static final int BUFFER_MULTIPLIER = 10;

	private ServerCommunication() {
	}

	/*
	 * Returns the size of a server read buffer.
	 */
	static int getServerReadBufferSize() {
		int max = NetworkProtocol.getMaxArgumentSize(Communicator.CLIENT, Communicator.SERVER);
		Logger.debug("SERVER READ BUFFER SIZE : " + max);
		return max;
	}

	/*
	 * Returns the size of a server write buffer.
	 */
	static int getServerWriteBufferSize() {
		int max = NetworkProtocol.getMaxRequestSize(Communicator.SERVER, Communicator.CLIENT) * BUFFER_MULTIPLIER;
		Logger.debug("SERVER WRITE BUFFER SIZE : " + max);
		return max;
	}

	/*
	 * Returns the size of the server broadcast buffer.
	 */
	static int getServerBroadcastBufferSize() {
		int max = NetworkProtocol.getMaxRequestSize(Communicator.SERVER, Communicator.CLIENT);
		Logger.debug("SERVER BROADCAST BUFFER SIZE : " + max);
		return max;
	}

	/*
	 * Reads a string by decoding this buffer with the protocol charset.
	 * 
	 * [!] The ByteBuffer argument must be in read mode.
	 */
	static String decodeString(ByteBuffer bb) {
		return PROTOCOL_CHARSET.decode(bb).toString();
	}

	/*
	 * Adds an ERROR request to the buffer.
	 */
	static boolean addRequestERROR(ByteBuffer bbWrite, ErrorType type) {
		int length = Integer.BYTES + Integer.BYTES;
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.ERROR.ordinal());
		bbWrite.putInt(type.ordinal());

		return true;
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
	 * Adds a MSGBC request to the buffer.
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
	 * Adds a CONOTIF request to the buffer.
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
	 * Adds a DISCONOTIF request to the buffer.
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

	/*
	 * Adds an PVCOREQNOTIF request to the buffer.
	 */
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

	/*
	 * Adds an PVCOESTASRC request to the buffer.
	 */
	static boolean addRequestPVCOESTASRC(ByteBuffer bbWrite, String username, InetAddress address) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);
		byte[] addr = address.getAddress();

		int sizeUsername = encodedUsername.remaining();
		int sizeAddress = addr.length;
		int length = Integer.BYTES + Integer.BYTES + sizeUsername + Integer.BYTES + sizeAddress;
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.PVCOESTASRC.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);
		bbWrite.putInt(sizeAddress);
		for (byte b : addr) {
			bbWrite.put(b);
		}

		return true;
	}

	/*
	 * Adds an PVCOESTADST request to the buffer.
	 */
	static boolean addRequestPVCOESTADST(ByteBuffer bbWrite, String username, InetAddress address, int portMessage,
			int portFile) {
		ByteBuffer encodedUsername = PROTOCOL_CHARSET.encode(username);
		byte[] addr = address.getAddress();

		int sizeUsername = encodedUsername.remaining();
		int sizeAddress = addr.length;
		int length = Integer.BYTES + Integer.BYTES + sizeUsername + Integer.BYTES + sizeAddress + (2 * Integer.BYTES);
		if (bbWrite.remaining() < length) {
			return false;
		}

		bbWrite.putInt(NetworkProtocol.PVCOESTADST.ordinal());
		bbWrite.putInt(sizeUsername).put(encodedUsername);
		bbWrite.putInt(addr.length);
		for (byte b : addr) {
			bbWrite.put(b);
		}
		bbWrite.putInt(portMessage).putInt(portFile);

		return true;
	}

}
