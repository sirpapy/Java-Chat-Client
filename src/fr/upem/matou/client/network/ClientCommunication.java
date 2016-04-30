package fr.upem.matou.client.network;

import static fr.upem.matou.shared.logger.Logger.formatNetworkData;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class consists only of static methods. These methods are used by the client to ensure that communications meet
 * the protocol.
 * 
 * All "sendRequest" methods check arguments validity and return false if at least one argument is not valid.
 * 
 * All "receiveRequest" methods throw an IOException if the SocketChannel is closed.
 */
class ClientCommunication {

	private static final Charset PROTOCOL_CHARSET = NetworkCommunication.getProtocolCharset();
	private static final int USERNAME_MAX_SIZE = NetworkCommunication.getUsernameMaxSize();
	private static final int MESSAGE_MAX_SIZE = NetworkCommunication.getMessageMaxSize();
	private static final int FILENAME_MAX_SIZE = NetworkCommunication.getFilenameMaxSize();
	private static final int CHUNK_SIZE = NetworkCommunication.getFileChunkSize();

	private static final Path FILE_PATH = Paths.get("./files/");
	private static final String FILENAME_SEPARATOR = "_";

	private ClientCommunication() {
	}

	/*
	 * Accepts a pending connection to the given address. All other pending connections are refused.
	 * 
	 * The ServerSocketChannel will be closed after this call.
	 */
	static SocketChannel acceptConnection(ServerSocketChannel ssc, InetAddress address) throws IOException {
		try (ServerSocketChannel listening = ssc) {
			SocketChannel pv;
			while (true) {
				pv = ssc.accept();
				InetAddress connected = ((InetSocketAddress) pv.getRemoteAddress()).getAddress();
				if (!address.equals(connected)) { // the accepted address is not the expected address
					Logger.debug(formatNetworkData(pv, "CONNECTION REFUSED"));
					NetworkCommunication.silentlyClose(pv);
					continue;
				}
				Logger.debug(formatNetworkData(pv, "CONNECTION ACCEPTED"));
				return pv;
			}
		} // close the ssc correctly
	}

	/*
	 * Writes all the given buffer (in write mode).
	 */
	private static void writeFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	/*
	 * Writes a protocol type.
	 */
	static void writeProtocol(SocketChannel sc, NetworkProtocol protocol) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		bb.putInt(protocol.ordinal());
		writeFully(sc, bb);
	}

	/*
	 * Writes an integer.
	 */
	static void writeInt(SocketChannel sc, int value) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		bb.putInt(value);
		writeFully(sc, bb);
	}

	/*
	 * Writes a long.
	 */
	static void writeLong(SocketChannel sc, long value) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
		bb.putLong(value);
		writeFully(sc, bb);
	}

	/*
	 * Writes an encoded string.
	 * 
	 * [!] The ByteBuffer argument must be in read mode.
	 */
	static void writeString(SocketChannel sc, ByteBuffer encoded) throws IOException {
		int size = encoded.remaining();
		writeInt(sc, size);
		encoded.compact();
		writeFully(sc, encoded);
	}

	/*
	 * Writes a string. The protocol charset will be used to encode the string.
	 * 
	 * This method is for crash test only (the string validity is not checked here).
	 */
	static void writeString(SocketChannel sc, String string) throws IOException {
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(string);
		writeString(sc, encoded);
	}

	/*
	 * Writes file by chunks.
	 */
	private static void writeFileChunks(SocketChannel sc, Path path) throws IOException {
		Logger.debug(formatNetworkData(sc, "FILE UPLOADING START : " + path));
		try (InputStream is = Files.newInputStream(path, READ)) {
			byte[] chunk = new byte[CHUNK_SIZE];
			int read = 0;
			while ((read = is.read(chunk)) != -1) { // Reads and writes by chunks
				ByteBuffer wrap = ByteBuffer.wrap(chunk, 0, read);
				sc.write(wrap);
			}
		}
		Logger.debug(formatNetworkData(sc, "FILE UPLOADING END : " + path));
	}

	/*
	 * Sends a COREQ request.
	 */
	static boolean sendRequestCOREQ(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		writeProtocol(sc, NetworkProtocol.COREQ);
		writeString(sc, optional.get());
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
		writeProtocol(sc, NetworkProtocol.MSG);
		writeString(sc, optional.get());
		return true;
	}

	/*
	 * Sends a PVCOREQ request.
	 */
	static boolean sendRequestPVCOREQ(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		writeProtocol(sc, NetworkProtocol.PVCOREQ);
		writeString(sc, optional.get());
		return true;
	}

	/*
	 * Sends a PVCOACC request.
	 */
	static boolean sendRequestPVCOACC(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		writeProtocol(sc, NetworkProtocol.PVCOACC);
		writeString(sc, optional.get());
		return true;
	}

	/*
	 * Sends a PVCOPORT request.
	 */
	static boolean sendRequestPVCOPORT(SocketChannel sc, String username, int portMessage, int portFile)
			throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		writeProtocol(sc, NetworkProtocol.PVCOPORT);
		writeString(sc, optional.get());
		writeInt(sc, portMessage);
		writeInt(sc, portFile);
		return true;
	}

	/*
	 * Sends a PVMSG request.
	 */
	static boolean sendRequestPVMSG(SocketChannel sc, String message) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}
		writeProtocol(sc, NetworkProtocol.PVMSG);
		writeString(sc, optional.get());
		return true;
	}

	/*
	 * Sends a PVFILE request.
	 */
	static boolean sendRequestPVFILE(SocketChannel sc, Path path) throws IOException {
		try {

			long totalSize = Files.size(path);
			Optional<ByteBuffer> optional = NetworkCommunication.encodePath(path.getFileName());
			if (!optional.isPresent()) {
				return false;
			}
			writeProtocol(sc, NetworkProtocol.PVFILE);
			writeString(sc, optional.get());
			writeLong(sc, totalSize);

			new Thread(() -> {
				try {
					writeFileChunks(sc, path);
				} catch (IOException e) {
					Logger.warning(e.toString());
				}
			}, "private file uploader : " + path).start();

			return true;

		} catch (@SuppressWarnings("unused") NoSuchFileException __) {
			Logger.warning("This file does not exist : " + path);
			return false;
		}
	}

	/*
	 * Reads buffer until it is full. If the channel is closed, an IOException is thrown.
	 */
	private static void readFully(SocketChannel sc, ByteBuffer bb) throws IOException {
		while (bb.hasRemaining()) {
			int read = sc.read(bb);
			if (read == -1) {
				throw new IOException("Connection closed");
			}
		}
	}

	/*
	 * Reads a byte.
	 */
	private static byte readByte(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Byte.BYTES);
		readFully(sc, bb);
		bb.flip();
		return bb.get();
	}

	/*
	 * Reads an integer.
	 */
	private static int readInt(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		readFully(sc, bb);
		bb.flip();
		return bb.getInt();
	}

	/*
	 * Reads a long.
	 */
	private static long readLong(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
		readFully(sc, bb);
		bb.flip();
		return bb.getLong();
	}

	/*
	 * Reads a string of a fixed size.
	 */
	private static String readString(SocketChannel sc, int size) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(size);
		readFully(sc, bb);
		bb.flip();
		return PROTOCOL_CHARSET.decode(bb).toString();
	}

	/*
	 * Reads a username (preceded by its size). Size is checked before reading.
	 */
	private static Username readUsername(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if (size > USERNAME_MAX_SIZE) {
			throw new IOException("Protocol violation - Invalid username size : " + size);
		}
		return new Username(readString(sc, size));
	}

	/*
	 * Reads a message (preceded by its size). Size is checked before reading.
	 */
	private static String readMessage(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if (size > MESSAGE_MAX_SIZE) {
			throw new IOException("Protocol violation - Invalid message size : " + size);
		}
		return readString(sc, size);
	}

	/*
	 * Reads a file name (preceded by its size). Size is checked before reading.
	 */
	private static String readFilename(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if (size > FILENAME_MAX_SIZE) {
			throw new IOException("Protocol violation - Invalid filename size : " + size);
		}
		return readString(sc, size);
	}

	/*
	 * Reads an IP address (preceded by its size). Size is checked before reading.
	 */
	private static InetAddress readAddress(SocketChannel sc) throws IOException {
		int size = readInt(sc);
		if (size != 4 && size != 16) {
			throw new IOException("Protocol violation - Invalid address size : " + size);
		}
		ByteBuffer bb = ByteBuffer.allocate(size);
		readFully(sc, bb);
		bb.flip();
		byte[] addr = new byte[size];
		for (int i = 0; i < size; i++) {
			byte b = bb.get();
			addr[i] = b;
		}
		return InetAddress.getByAddress(addr);
	}

	/*
	 * Reads a file by chunks and writes it in the file system.
	 */
	private static void saveFileChunks(SocketChannel sc, Path path, long totalSize) throws IOException {
		try (OutputStream os = Files.newOutputStream(path, WRITE, CREATE, TRUNCATE_EXISTING)) {
			ByteBuffer bb = ByteBuffer.allocate(CHUNK_SIZE);
			for (long totalRead = 0; totalRead < totalSize;) {
				long diff = totalSize - totalRead; // remaining bytes to read
				long capacity = diff <= CHUNK_SIZE ? diff : CHUNK_SIZE; // bytes of the directly next chunk
				bb.limit((int) capacity);
				readFully(sc, bb);
				bb.flip();
				byte[] chunk = bb.array();
				int read = bb.remaining();
				totalRead += read; // updates the number of bytes already read
				os.write(chunk, 0, read);
			}
		}
	}

	/*
	 * Receives a protocol type request.
	 */
	static NetworkProtocol receiveRequestType(SocketChannel sc) throws IOException {
		int ordinal = readInt(sc);
		Optional<NetworkProtocol> protocol = NetworkProtocol.getProtocol(ordinal);
		if (!protocol.isPresent()) {
			throw new IOException("Protocol violation - Invalid protocol type : " + ordinal);
		}
		return protocol.get();
	}

	/*
	 * Receives an ERROR request.
	 */
	static ErrorType receiveRequestERROR(SocketChannel sc) throws IOException {
		int ordinal = readInt(sc);
		Optional<ErrorType> error = ErrorType.getError(ordinal);
		if (!error.isPresent()) {
			throw new IOException("Protocol violation - Invalid error type : " + ordinal);
		}
		return error.get();
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
		Username username = readUsername(sc);
		String message = readMessage(sc);
		return new Message(username, message);
	}

	/*
	 * Receives a CONOTIF request.
	 */
	static Username receiveRequestCONOTIF(SocketChannel sc) throws IOException {
		Username username = readUsername(sc);
		return username;
	}

	/*
	 * Receives a DISCONOTIF request.
	 */
	static Username receiveRequestDISCONOTIF(SocketChannel sc) throws IOException {
		Username username = readUsername(sc);
		return username;
	}

	/*
	 * Receives a PVCOREQNOTIF request.
	 */
	static Username receiveRequestPVCOREQNOTIF(SocketChannel sc) throws IOException {
		Username username = readUsername(sc);
		return username;
	}

	/*
	 * Receives a PVCOESTASRC request.
	 */
	static SourceConnectionData receiveRequestPVCOESTASRC(SocketChannel sc) throws IOException {
		Username username = readUsername(sc);
		InetAddress address = readAddress(sc);
		return new SourceConnectionData(username, address);
	}

	/*
	 * Receives a PVCOESTADST request.
	 */
	static DestinationConnectionData receiveRequestPVCOESTADST(SocketChannel sc) throws IOException {
		Username username = readUsername(sc);
		InetAddress address = readAddress(sc);
		int portMessage = readInt(sc);
		int portFile = readInt(sc);
		return new DestinationConnectionData(username, address, portMessage, portFile);
	}

	/*
	 * Receives a PVMSG request.
	 */
	static Message receiveRequestPVMSG(SocketChannel sc, Username username) throws IOException {
		String message = readMessage(sc);
		return new Message(username, message, true);
	}

	/*
	 * Receives a PVFILE request.
	 */
	static Path receiveRequestPVFILE(SocketChannel sc, String username) throws IOException {
		String filename = readFilename(sc);
		long totalSize = readLong(sc);

		// Ensures creation of a new file
		Path path = Files.createTempFile(FILE_PATH, username + FILENAME_SEPARATOR, FILENAME_SEPARATOR + filename);

		Logger.debug(formatNetworkData(sc, "FILE DOWNLOADING START : " + path));
		saveFileChunks(sc, path, totalSize);
		Logger.debug(formatNetworkData(sc, "FILE DOWNLOADING END : " + path));

		return path;
	}

}
