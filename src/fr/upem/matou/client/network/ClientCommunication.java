package fr.upem.matou.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import fr.upem.matou.client.ui.Message;
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

	public static void sendRequest(SocketChannel sc, ByteBuffer bb) throws IOException {
		bb.flip();
		sc.write(bb);
	}

	/*
	 * Encodes a COREQ request.
	 */
	public static ByteBuffer encodeRequestCOREQ(ByteBuffer encodedUsername) {
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
	public static ByteBuffer encodeRequestMSG(ByteBuffer encodedMessage) {
		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.MSG.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	public static ByteBuffer encodeRequestDISCO() {
		int capacity = Integer.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.DISCO.ordinal());

		return request;
	}

	public static ByteBuffer encodeRequestPVCOREQ(ByteBuffer encodedUsername) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOREQ.ordinal());
		request.putInt(length).put(encodedUsername);

		return request;
	}

	public static ByteBuffer encodeRequestPVCOACC(ByteBuffer encodedUsername) {
		int length = encodedUsername.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOACC.ordinal());
		request.putInt(length).put(encodedUsername);

		return request;
	}

	public static ByteBuffer encodeRequestPVCOPORT(ByteBuffer encodedUsername, InetAddress address, int portMessage,
			int portFile) {
		int length = encodedUsername.remaining();
		byte[] addr = address.getAddress();

		int capacity = Integer.BYTES + Integer.BYTES + length + Integer.BYTES + addr.length + (2 * Integer.BYTES);
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVCOPORT.ordinal());
		request.putInt(length).put(encodedUsername);
		request.putInt(addr.length).put(addr);
		request.putInt(portMessage).putInt(portFile);

		return request;
	}

	public static ByteBuffer encodeRequestPVMSG(ByteBuffer encodedMessage) {
		int length = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + length;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVMSG.ordinal());
		request.putInt(length).put(encodedMessage);

		return request;
	}

	private static ByteBuffer encodeRequestPVFILE(long totalSize) {
		int capacity = Integer.BYTES + Long.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.PVFILE.ordinal());
		request.putLong(totalSize);

		return request;
	}

	/*
	 * Sends a COREQ request.
	 */
	public static boolean sendRequestCOREQ(SocketChannel sc, String username) throws IOException {
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
	public static boolean sendRequestMSG(SocketChannel sc, String message) throws IOException {
		if (!NetworkCommunication.checkMessageValidity(message)) {
			return false;
		}

		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}

		ByteBuffer bb = encodeRequestMSG(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	public static void sendRequestDISCO(SocketChannel sc) throws IOException {
		ByteBuffer bb = encodeRequestDISCO();
		sendRequest(sc, bb);
	}

	public static boolean sendRequestPVCOREQ(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOREQ(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	public static boolean sendRequestPVCOACC(SocketChannel sc, String username) throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOACC(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	public static boolean sendRequestPVCOPORT(SocketChannel sc, String username, InetAddress address, int portMessage,
			int portFile)
			throws IOException {
		Optional<ByteBuffer> optional = NetworkCommunication.encodeUsername(username);
		if (!optional.isPresent()) {
			return false;
		}
		ByteBuffer bb = encodeRequestPVCOPORT(optional.get(), address, portMessage, portFile);
		sendRequest(sc, bb);
		return true;
	}

	public static boolean sendRequestPVMSG(SocketChannel sc, String message) throws IOException {
		if (!NetworkCommunication.checkMessageValidity(message)) {
			return false;
		}

		Optional<ByteBuffer> optional = NetworkCommunication.encodeMessage(message);
		if (!optional.isPresent()) {
			return false;
		}

		ByteBuffer bb = encodeRequestPVMSG(optional.get());
		sendRequest(sc, bb);
		return true;
	}

	public static void sendRequestPVFILE(SocketChannel sc, Path path) throws IOException {
		long totalSize = Files.size(path);
		ByteBuffer bb = encodeRequestPVFILE(totalSize);
		sendRequest(sc, bb);

		try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
			byte[] chunk = new byte[CHUNK_SIZE];
			int read = 0;
			long totalRead = 0;
			while ((read = is.read(chunk)) != -1) {
				totalRead += read;
				long percent = totalRead * 100 / totalSize;
				System.out.println(
						"READ LENGTH : " + read + "\n\tTotal : " + totalRead + "/" + totalSize + " [" + percent + "%]");
				ByteBuffer wrap = ByteBuffer.wrap(chunk, 0, read);
				// TODO : flip ?
				sc.write(wrap);
			}
		}
	}

	/*
	 * Receives a protocol type request.
	 */
	public static Optional<NetworkProtocol> receiveRequestType(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		int ordinal = bb.getInt();
		return NetworkProtocol.getProtocol(ordinal);
	}

	/*
	 * Receives a CORES request.
	 */
	public static Optional<Boolean> receiveRequestCORES(SocketChannel sc) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1);
		if (!readFully(sc, bb)) {
			throw new IOException("Connection closed");
		}
		bb.flip();
		byte acceptation = bb.get();
		return Optional.of(acceptation != 0);
	}

	/*
	 * Receives a MSGBG request.
	 */
	public static Optional<Message> receiveRequestMSGBC(SocketChannel sc) throws IOException {
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

		return Optional.of(new Message(username, message));
	}

	/*
	 * Receives a CONOTIF request.
	 */
	public static Optional<String> receiveRequestCONOTIF(SocketChannel sc) throws IOException {
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

		return Optional.of(username);
	}

	/*
	 * Receives a DISCONOTIF request.
	 */
	public static Optional<String> receiveRequestDISCONOTIF(SocketChannel sc) throws IOException {
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

		return Optional.of(username);
	}

	public static Optional<String> receiveRequestPVCOREQNOTIF(SocketChannel sc) throws IOException {
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

		return Optional.of(username);
	}

	public static Optional<SourceConnection> receiveRequestPVCOESTASRC(SocketChannel sc) throws IOException {
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
			System.out.println("BYTE ADDRESS = " + b);
			addr[i] = b;
		}
		InetAddress address = InetAddress.getByAddress(addr);
		System.out.println("ADDRESS = " + address);

		return Optional.of(new SourceConnection(address, username));
	}

	public static Optional<DestinationConnection> receiveRequestPVCOESTADST(SocketChannel sc) throws IOException {
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
			System.out.println("BYTE ADDRESS = " + b);
			addr[i] = b;
		}
		InetAddress address = InetAddress.getByAddress(addr);
		System.out.println("ADDRESS = " + address);

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

		return Optional.of(new DestinationConnection(username, address, portMessage, portFile));
	}

	public static Optional<Message> receiveRequestPVMSG(SocketChannel sc, String username) throws IOException {
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

		return Optional.of(new Message(username, message, true));
	}

	public static Optional<Path> receiveRequestPVFILE(SocketChannel sc, String username) throws IOException {
		ByteBuffer bbSizeFile = ByteBuffer.allocate(Long.BYTES);
		if (!readFully(sc, bbSizeFile)) {
			throw new IOException("Connection closed");
		}
		bbSizeFile.flip();
		long totalSize = bbSizeFile.getLong();

		Path path = Paths.get(username + "_FILE");
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW)) {
			long totalRead = 0;
			while (totalRead < totalSize) {
				ByteBuffer bbChunk = ByteBuffer.allocate(CHUNK_SIZE);
				if (!readFully(sc, bbChunk)) {
					throw new IOException("Connection closed");
				}
				bbChunk.flip();
				byte[] chunk = bbChunk.array();
				int read = bbChunk.remaining();				
				totalRead += read;
				long percent = totalRead * 100 / totalSize;
				System.out.println("READ LENGTH : " + read + "\n\tTotal : " + totalRead + "/" + totalSize + " [" + percent + "%]");
				os.write(chunk, 0, read);
			}
		}
		
		return Optional.of(path);
	}

}
