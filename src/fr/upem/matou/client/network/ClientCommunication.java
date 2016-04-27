package fr.upem.matou.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

/*
 * This class consists only of static methods.
 * These methods are used by the client to ensure that communications meet the protocol.
 */
class ClientCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();
	private static final int CHUNK_SIZE = NetworkCommunication.getFileChunkSize();

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

	static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	/*
	 * Encodes a COREQ request.
	 */
	static ByteBuffer encodeRequestCOREQ(ByteBuffer encodedUsername) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.COREQ.ordinal());
		request.putInt(length).put(encodedUsername);

		return request;
	}

	/*
	 * Encodes a MSG request.
	 */
	static ByteBuffer encodeRequestMSG(ByteBuffer encodedMessage) {
		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.MSG.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	static ByteBuffer encodeRequestPVCOREQ(ByteBuffer encodedUsername) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOREQ.ordinal());
		request.putInt(length).put(encodedUsername);

		return request;
	}

	static ByteBuffer encodeRequestPVCOACC(ByteBuffer encodedUsername) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOACC.ordinal());
		request.putInt(length).put(encodedUsername);

		return request;
	}

	static ByteBuffer encodeRequestPVCOPORT(ByteBuffer encodedUsername, int portMessage,
			int portFile) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length + (2 * Integer.BYTES);
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOPORT.ordinal());
		request.putInt(length).put(encodedUsername);
		request.putInt(portMessage).putInt(portFile);

		return request;
	}

	static ByteBuffer encodeRequestPVMSG(ByteBuffer encodedMessage) {
		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVMSG.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	static ByteBuffer encodeRequestPVFILE(ByteBuffer encodedPath, long totalSize) {
		int length = encodedPath.remaining();
		
		int capacity = Integer.BYTES + Integer.BYTES + length + Long.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVFILE.ordinal());
		request.putInt(length).put(encodedPath);
		request.putLong(totalSize);

		return request;
	}

	/*
	 * Sends a COREQ request.
	 */
	static boolean sendRequestCOREQ(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
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
	static boolean sendRequestMSG(SocketChannel sc, String message) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}

		ByteBuffer bb = encodeRequestMSG(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	static boolean sendRequestPVCOREQ(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOREQ(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	static boolean sendRequestPVCOACC(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOACC(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	static boolean sendRequestPVCOPORT(SocketChannel sc, String username, int portMessage, int portFile)
			throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOPORT(optional.get(), portMessage, portFile);
		sendRequest(sc, bb);
		return true;
	}

	static boolean sendRequestPVMSG(SocketChannel sc, String message) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}

		ByteBuffer bb = encodeRequestPVMSG(optional.get());
		sendRequest(sc, bb);
		return true;
	}
	
	static boolean sendRequestPVFILE(SocketChannel sc, Path path) throws IOException {
		try {

			long totalSize = Files.size(path);
			Optional<ByteBuffer> optional = NetworkCommunication.encodePath(path.getFileName());
			if(!optional.isPresent()) {
				return false;
			}
			ByteBuffer bb = encodeRequestPVFILE(optional.get(),totalSize);
			sendRequest(sc, bb);

			new Thread(() -> {
				try {
					sendFileChunks(sc, path);
				} catch (Exception e) {
					Logger.exception(e);
				}
			}).start();

			return true;

		} catch (@SuppressWarnings("unused") NoSuchFileException __) {
			Logger.warning("File \"" + path + "\" does not exist");
			return false;
		}
	}

	private static void sendFileChunks(SocketChannel sc, Path path) throws IOException {
		Logger.debug("FILE UPLOADING : START");
		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
			byte[] chunk = new byte[CHUNK_SIZE];
			int read = 0;
			while ((read = is.read(chunk)) != -1) {
				ByteBuffer wrap = ByteBuffer.wrap(chunk, 0, read);
				sc.write(wrap);
			}
		}
		Logger.debug("FILE UPLOADING : END");
	}
	
	/*
	 * Receives a protocol type request.
	 */
	static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		int ordinal = bb.getInt();
		return NetworkProtocol.getProtocol(ordinal);
	}

	static Optional<ErrorType> receiveRequestERROR(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		int ordinal = bb.getInt();
		return ErrorType.getError(ordinal);
	}
	
	/*
	 * Receives a CORES request.
	 */
	static boolean receiveRequestCORES(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		byte acceptation = bb.get();
		return acceptation != 0;
	}

	/*
	 * Receives a MSGBG request.
	 */
	static Message receiveRequestMSGBC(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

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

		return new Message(username, message);
	}

	/*
	 * Receives a CONOTIF request.
	 */
	static String receiveRequestCONOTIF(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

		return username;
	}

	/*
	 * Receives a DISCONOTIF request.
	 */
	static String receiveRequestDISCONOTIF(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

		return username;
	}

	static String receiveRequestPVCOREQNOTIF(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

		return username;
	}

	static SourceConnection receiveRequestPVCOESTASRC(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

		ByteBuffer bbSizeAddress = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeAddress)) {
			throw new IOException("Connection closed");
		}
		bbSizeAddress.flip();
		int sizeAddress = bbSizeAddress.getInt();

		ByteBuffer bbAddress = ByteBuffer.allocate(sizeAddress);
		if (!readFully(sc, bbAddress)) {
			throw new IOException("Connection closed");
		}
		bbAddress.flip();
		byte[] addr = new byte[sizeAddress];
		for (int i = 0; i < sizeAddress; i++) {
			byte b = bbAddress.get();
			addr[i] = b;
		}
		Logger.debug("ADDRESS = " + Arrays.toString(addr));
		InetAddress address = InetAddress.getByAddress(addr);

		return new SourceConnection(username, address);
	}

	static DestinationConnection receiveRequestPVCOESTADST(SocketChannel sc) throws IOException {
		ByteBuffer bbSizeUsername = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeUsername)) {
			throw new IOException("Connection closed");
		}
		bbSizeUsername.flip();
		int sizeUsername = bbSizeUsername.getInt();

		ByteBuffer bbUsername = ByteBuffer.allocate(sizeUsername);
		if (!readFully(sc, bbUsername)) {
			throw new IOException("Connection closed");
		}
		bbUsername.flip();
		String username = PROTOCOL_CHARSET.decode(bbUsername).toString();

		ByteBuffer bbSizeAddress = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeAddress)) {
			throw new IOException("Connection closed");
		}
		bbSizeAddress.flip();
		int sizeAddress = bbSizeAddress.getInt();

		ByteBuffer bbAddress = ByteBuffer.allocate(sizeAddress);
		if (!readFully(sc, bbAddress)) {
			throw new IOException("Connection closed");
		}
		bbAddress.flip();
		byte[] addr = new byte[sizeAddress];
		for (int i = 0; i < sizeAddress; i++) {
			byte b = bbAddress.get();
			addr[i] = b;
		}
		Logger.debug("ADDRESS = " + Arrays.toString(addr));
		InetAddress address = InetAddress.getByAddress(addr);

		ByteBuffer bbPortMessage = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbPortMessage)) {
			throw new IOException("Connection closed");
		}
		bbPortMessage.flip();
		int portMessage = bbPortMessage.getInt();

		ByteBuffer bbPortFile = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbPortFile)) {
			throw new IOException("Connection closed");
		}
		bbPortFile.flip();
		int portFile = bbPortFile.getInt();

		return new DestinationConnection(username, address, portMessage, portFile);
	}

	static Message receiveRequestPVMSG(SocketChannel sc, String username) throws IOException {
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

		return new Message(username, message, true);
	}

	static Path receiveRequestPVFILE(SocketChannel sc, String username) throws IOException {
		ByteBuffer bbSizeName = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bbSizeName)) {
			throw new IOException("Connection closed");
		}
		bbSizeName.flip();
		int sizeName = bbSizeName.getInt();
		
		ByteBuffer bbName = ByteBuffer.allocate(sizeName);
		if (!readFully(sc, bbName)) {
			throw new IOException("Connection closed");
		}
		bbName.flip();
		String filename = PROTOCOL_CHARSET.decode(bbName).toString();

		ByteBuffer bbSizeFile = ByteBuffer.allocate(Long.BYTES);
		if (!readFully(sc, bbSizeFile)) {
			throw new IOException("Connection closed");
		}
		bbSizeFile.flip();
		long totalSize = bbSizeFile.getLong();
		
		Logger.debug("FILE DOWNLOADING : START");
		Path path = Files.createTempFile(Paths.get("./files"), username + "_", "_" + filename);
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			long totalRead = 0;
			while (totalRead < totalSize) {
				long diff = totalSize - totalRead;
				long capacity = diff <= CHUNK_SIZE ? diff : CHUNK_SIZE; // bytes of the next chunk
				ByteBuffer bbChunk = ByteBuffer.allocate((int) capacity);
				if (!readFully(sc, bbChunk)) {
					throw new IOException("Connection closed");
				}
				bbChunk.flip();
				byte[] chunk = bbChunk.array();
				int read = bbChunk.remaining();
				totalRead += read;
				os.write(chunk, 0, read);
			}
		}

		Logger.debug("FILE DOWNLOADING : END");
		return path;
	}

}
