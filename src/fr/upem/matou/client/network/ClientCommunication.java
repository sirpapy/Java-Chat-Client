package fr.upem.matou.client.network;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

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
 * This class consists only of static methods. These methods are used by the client to ensure that communications meet
 * the protocol.
 */
class ClientCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();
	private static final int USERNAME_MAX_SIZE = NetworkCommunication.getUsernameMaxSize();
	private static final int MESSAGE_MAX_SIZE = NetworkCommunication.getMessageMaxSize();
	private static final int FILENAME_MAX_SIZE = NetworkCommunication.getFilenameMaxSize();
	private static final int CHUNK_SIZE = NetworkCommunication.getFileChunkSize();

	private ClientCommunication() {
	}

	private static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
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

	static ByteBuffer encodeRequestPVCOPORT(ByteBuffer encodedUsername, int portMessage, int portFile) {
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
			if (!optional.isPresent()) {
				return false;
			}
			ByteBuffer bb = encodeRequestPVFILE(optional.get(), totalSize);
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

	private static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while (bb.hasRemaining()) {
			int read = sc.read(bb);
			if (read == -1) {
				return false;
			}
		}
		return true;
	}

	private static byte readByte(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		return bb.get();
	}

	private static int readInt(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		return bb.getInt();
	}

	private static long readLong(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		return bb.getLong();
	}

	private static String readString(SocketChannel sc, int size) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(size); // FIXME : Taille trop grosse ?
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		return PROTOCOL_CHARSET.decode(bb).toString();
	}
	
	private static String readUsername(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if(size > USERNAME_MAX_SIZE) {
			throw new IOException("Protocol violation");
		}
		return readString(sc, size);
	}
	
	private static String readMessage(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if(size > MESSAGE_MAX_SIZE) {
			throw new IOException("Protocol violation");
		}
		return readString(sc, size);
	}
	
	private static String readFilename(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if(size > FILENAME_MAX_SIZE) {
			throw new IOException("Protocol violation");
		}
		return readString(sc, size);
	}

	// TODO : readUsername & readMessage <= readString

	private static InetAddress readAddress(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		ByteBuffer bbAddress = ByteBuffer.allocate(size);
		if (!readFully(sc, bbAddress)) {
			throw new IOException("Connection closed");
		}
		bbAddress.flip();
		byte[] addr = new byte[size];
		for (int i = 0; i < size; i++) {
			byte b = bbAddress.get();
			addr[i] = b;
		}
		Logger.debug("ADDRESS = " + Arrays.toString(addr));
		return InetAddress.getByAddress(addr);
	}

	private static void saveFileChunks(SocketChannel sc, Path path, long totalSize) throws IOException {
		try (OutputStream os = Files.newOutputStream(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
			for (long totalRead = 0; totalRead < totalSize;) {
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
	}

	/*
	 * Receives a protocol type request.
	 */
	static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		int ordinal = readInt(sc);
		return NetworkProtocol.getProtocol(ordinal);
	}

	static Optional<ErrorType> receiveRequestERROR(SocketChannel sc) throws IOException {
		int ordinal = readInt(sc);
		return ErrorType.getError(ordinal);
	}

	/*
	 * Receives a CORES request.
	 */
	static boolean receiveRequestCORES(SocketChannel sc) throws IOException {
		byte acceptation = readByte(sc);
		return acceptation != 0;
	}

	/*
	 * Receives a MSGBG request.
	 */
	static Message receiveRequestMSGBC(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		String message = readMessage(sc);
		return new Message(username, message);
	}

	/*
	 * Receives a CONOTIF request.
	 */
	static String receiveRequestCONOTIF(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		return username;
	}

	/*
	 * Receives a DISCONOTIF request.
	 */
	static String receiveRequestDISCONOTIF(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		return username;
	}

	static String receiveRequestPVCOREQNOTIF(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		return username;
	}

	static SourceConnection receiveRequestPVCOESTASRC(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		InetAddress address = readAddress(sc);
		return new SourceConnection(username, address);
	}

	static DestinationConnection receiveRequestPVCOESTADST(SocketChannel sc) throws IOException {
		String username = readUsername(sc);
		InetAddress address = readAddress(sc);
		int portMessage = readInt(sc);
		int portFile = readInt(sc);
		return new DestinationConnection(username, address, portMessage, portFile);
	}

	static Message receiveRequestPVMSG(SocketChannel sc, String username) throws IOException {
		String message = readMessage(sc);
		return new Message(username, message, true);
	}

	static Path receiveRequestPVFILE(SocketChannel sc, String username) throws IOException {
		String filename = readFilename(sc);
		long totalSize = readLong(sc);

		Logger.debug("FILE DOWNLOADING : START");
		Path path = Files.createTempFile(Paths.get("./files"), username + "_", "_" + filename);
		saveFileChunks(sc, path, totalSize);

		Logger.debug("FILE DOWNLOADING : END");
		return path;
	}

}
