package fr.upem.matou.server.network;

import static fr.upem.matou.shared.logger.Logger.formatNetworkRequest;
import static fr.upem.matou.shared.logger.Logger.formatNetworkData;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;
import fr.upem.matou.shared.utils.ByteBuffers;

/*
 * This class represents the state of a client connected to the chat server. A ServerSession is always attached to one
 * and only one ServerDataBase and should be created by ServerDataBase.newServerSession(SocketChannel,SelectionKey).
 */
class ServerSession {

	private static final int BUFFER_SIZE_INPUT = ServerCommunication.getServerReadBufferSize();
	private static final int BUFFER_SIZE_OUTPUT = ServerCommunication.getServerWriteBufferSize();
	private static final int USERNAME_MAX_SIZE = NetworkCommunication.getUsernameMaxSize();
	private static final int MESSAGE_MAX_SIZE = NetworkCommunication.getMessageMaxSize();

	private final ServerDataBase db;
	private final SocketChannel sc;
	private final InetAddress address;
	private final SelectionKey key;
	private final ByteBuffer bbRead = ByteBuffer.allocateDirect(BUFFER_SIZE_INPUT);
	private final ByteBuffer bbWrite = ByteBuffer.allocateDirect(BUFFER_SIZE_OUTPUT);

	private boolean authent = false; // If the client has a username
	private NetworkProtocol protocol = null;
	private ServerReader serverReader = null;
	private int arg = 0; // Argument number of the current reader

	static interface ServerReader {
		// Marker interface of readers
	}

	static class ReaderUsername implements ServerReader {
		int sizeUsername;
		Username username;
	}

	static class ReaderMessage implements ServerReader {
		int sizeMessage;
		String message;
	}

	static class ReaderPort implements ServerReader {
		int sizeUsername;
		Username username;
		int portMessage;
		int portFile;
	}

	ServerSession(ServerDataBase db, SocketChannel sc, SelectionKey key) throws IOException {
		this.db = db;
		this.sc = sc;
		this.key = key;
		this.address = ((InetSocketAddress) sc.getRemoteAddress()).getAddress();
		resetReadState();
	}

	ByteBuffer getReadBuffer() {
		return bbRead;
	}

	ByteBuffer getWriteBuffer() {
		return bbWrite;
	}

	boolean isAuthent() {
		return authent;
	}

	private void setAuthent() {
		authent = true;
	}

	/*
	 * Appends the given buffer to the write buffer (if there is enough place).
	 */
	void appendWriteBuffer(ByteBuffer bb) {
		boolean succeeded = ByteBuffers.append(bbWrite, bb);
		if (!succeeded) {
			Logger.warning(formatNetworkData(sc, "Unknown request lost : Write Buffer cannot hold it"));
		}
	}

	/*
	 * Clears this buffer and sets a new limit.
	 */
	private final static void clearAndLimit(ByteBuffer bb, int size) {
		bb.clear();
		bb.limit(size);
	}

	/*
	 * Resets the read state of this client in order to read another request.
	 */
	private final void resetReadState() {
		clearAndLimit(bbRead, Integer.BYTES);
		arg = 0;
		protocol = null;
		serverReader = null;
	}

	/*
	 * Updates the state of this client in order to read another argument of the current request.
	 */
	private void advanceReadState(int size) {
		clearAndLimit(bbRead, size);
		arg++;
	}

	/*
	 * Prepares the read state in order to read a Username.
	 */
	private void setReaderUsername() {
		serverReader = new ReaderUsername();
		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the first argument of the ReaderUsername and updates the current read state.
	 */
	private void readUsernameArg1(ReaderUsername reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername));
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.warning(formatNetworkData(sc, "Invalid size username"));
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	/*
	 * Process the read buffer to retrieves the second argument of the ReaderUsername and updates the current read
	 * state.
	 */
	private void readUsernameArg2(ReaderUsername reader) {
		bbRead.flip();
		String string = ServerCommunication.decodeString(bbRead);
		reader.username = new Username(string);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + reader.username));

		resetReadState();
	}

	/*
	 * Prepares the read state in order to read a Message.
	 */
	private void setReaderMessage() {
		serverReader = new ReaderMessage();
		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the first argument of the ReaderMessage and updates the current read state.
	 */
	private void readMessageArg1(ReaderMessage reader) {
		bbRead.flip();
		reader.sizeMessage = bbRead.getInt();
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "SIZE MESSAGE : " + reader.sizeMessage));
		if (reader.sizeMessage > MESSAGE_MAX_SIZE || reader.sizeMessage == 0) {
			Logger.warning(formatNetworkData(sc, "Invalid size message"));
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeMessage);
	}

	/*
	 * Process the read buffer to retrieves the second argument of the ReaderMessage and updates the current read state.
	 */
	private void readMessageArg2(ReaderMessage reader) {
		bbRead.flip();
		reader.message = ServerCommunication.decodeString(bbRead);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "MESSAGE : " + reader.message));

		resetReadState();
	}

	/*
	 * Prepares the read state in order to read a username and a two ports.
	 */
	private void setReaderPort() {
		serverReader = new ReaderPort();
		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the first argument of the PortReader and updates the current read state.
	 */
	private void readPortArg1(ReaderPort reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername));
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.warning(formatNetworkData(sc, "Invalid size username"));
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	/*
	 * Process the read buffer to retrieves the second argument of the PortReader and updates the current read state.
	 */
	private void readPortArg2(ReaderPort reader) {
		bbRead.flip();
		String string = ServerCommunication.decodeString(bbRead);
		reader.username = new Username(string);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "USERNAME : " + reader.username));

		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the third argument of the PortReader and updates the current read state.
	 */
	private void readPortArg3(ReaderPort reader) {
		bbRead.flip();
		reader.portMessage = bbRead.getInt();

		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the fourth argument of the PortReader and updates the current read state.
	 */
	private void readPortArg4(ReaderPort reader) {
		bbRead.flip();
		reader.portFile = bbRead.getInt();

		resetReadState();
	}

	/*
	 * Retrives the protocol request type from the read buffer and updates the current read state by setting the
	 * protocol type if valid.
	 */
	private int processRequestType() {
		bbRead.flip();
		int ordinal = bbRead.getInt();
		Optional<NetworkProtocol> optionalProtocol = NetworkProtocol.getProtocol(ordinal);
		if (!optionalProtocol.isPresent()) {
			return ordinal;
		}
		protocol = optionalProtocol.get();
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "PROTOCOL : " + protocol));
		return ordinal;
	}

	/*
	 * Answers by an ERROR request and fills the write buffer.
	 */
	private void answerERROR(ErrorType type) {
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.ERROR));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "ERROR : " + type));

		if (!ServerCommunication.addRequestERROR(bbWrite, type)) {
			Logger.warning(formatNetworkData(sc, "ERROR lost : Write Buffer cannot hold it"));
			return;
		}
	}

	/*
	 * Checks if the current state of the client is valid with a COREQ request.
	 */
	private boolean checkCOREQ() {
		return !isAuthent();
	}

	/*
	 * Process a COREQ request.
	 */
	private void processCOREQ() {
		if (arg == 0) {
			if (!checkCOREQ()) {
				Logger.warning(formatNetworkData(sc, "Client already authenticated"));
				disconnectClient();
				return;
			}
			setReaderUsername();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			readUsernameArg1(reader);
			return;
		case 2:
			readUsernameArg2(reader);
			answerCORES(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for COREQ");
		}
	}

	/*
	 * Answers by a CORES request and fills the write buffer.
	 */
	private void answerCORES(Username username) {
		if (!NetworkCommunication.checkUsernameValidity(username.toString())) {
			Logger.warning(formatNetworkData(sc, "INVALID USERNAME : " + username));
			disconnectClient();
			return;
		}

		boolean acceptation = db.authentClient(sc, username);
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.CORES));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "ACCEPTATION : " + acceptation));

		if (!ServerCommunication.addRequestCORES(bbWrite, acceptation)) {
			Logger.warning(formatNetworkData(sc, "CORES lost : Write Buffer cannot hold it"));
		}

		if (acceptation) {
			setAuthent();

			Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.CONOTIF));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));

			ByteBuffer bbWriteAll = db.getBroadcastBuffer();
			if (!ServerCommunication.addRequestCONOTIF(bbWriteAll, username.toString())) {
				Logger.warning(formatNetworkData(sc, "CONOTIF lost : Broadcast Buffer cannot hold it"));
				return;
			}
			db.updateStateReadAll();
		}
	}

	/*
	 * Checks if the current state of the client is valid with a MSG request.
	 */
	private boolean checkMSG() {
		return isAuthent();
	}

	/*
	 * Process a MSG request.
	 */
	private void processMSG() {
		if (arg == 0) {
			if (!checkMSG()) {
				Logger.warning(formatNetworkData(sc, "Client not authenticated"));
				disconnectClient();
				return;
			}
			setReaderMessage();
			return;
		}

		ReaderMessage reader = (ReaderMessage) serverReader;

		switch (arg) {
		case 1:
			readMessageArg1(reader);
			return;
		case 2:
			readMessageArg2(reader);
			answerMSGBC(reader.message);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for MSG");
		}
	}

	/*
	 * Answers by a MSGBC request and fills the broadcast buffer.
	 */
	private void answerMSGBC(String message) {
		if (!NetworkCommunication.checkMessageValidity(message)) {
			Logger.warning(formatNetworkData(sc, "INVALID MESSAGE : " + message));
			disconnectClient();
			return;
		}

		Username username = db.usernameOf(sc).get();
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSGBC));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));
		Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "MESSAGE : " + message));

		ByteBuffer bbWriteAll = db.getBroadcastBuffer();
		if (!ServerCommunication.addRequestMSGBC(bbWriteAll, username.toString(), message)) {
			Logger.warning(formatNetworkData(sc, "MSGBC lost : Broadcast Buffer cannot hold it"));
			return;
		}
		db.updateStateReadAll();
	}

	/*
	 * Checks if the current state of the client is valid with a PVCOREQ request.
	 */
	private boolean checkPVCOREQ() {
		return isAuthent();
	}

	/*
	 * Process a PVCOREQ request.
	 */
	private void processPVCOREQ() {
		if (arg == 0) {
			if (!checkPVCOREQ()) {
				Logger.warning(formatNetworkData(sc, "Client not authenticated"));
				disconnectClient();
				return;
			}
			setReaderUsername();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			readUsernameArg1(reader);
			return;
		case 2:
			readUsernameArg2(reader);
			answerPVCOREQNOTIF(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOREQ");
		}
	}

	/*
	 * Answers by a PVCOREQNOTIF request and fills the write buffer of the target.
	 */
	private void answerPVCOREQNOTIF(Username requested) {
		Username requester = db.usernameOf(sc).get();
		if (requested.equals(requester)) {
			return;
		}

		Optional<ServerSession> optional = db.sessionOf(requested);
		if (!optional.isPresent()) {
			Logger.debug(formatNetworkData(sc, "Target " + requested + " is not connected"));
			answerERROR(ErrorType.USRNOTCO);
			return;
		}

		boolean valid = db.addPrivateRequest(requester, requested);
		Logger.debug(formatNetworkData(sc, "PRIVATE REQUEST VALIDITY : " + valid));
		if (!valid) {
			return;
		}

		ServerSession session = optional.get();
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.info(
				formatNetworkRequest(session.sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOREQNOTIF));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "USERNAME : " + requester));

		if (!ServerCommunication.addRequestPVCOREQNOTIF(bbTarget, requester.toString())) {
			Logger.warning(formatNetworkData(session.sc, "PVCOREQNOTIF lost : Write Buffer cannot hold it"));
			return;
		}

		session.updateKey();
	}

	/*
	 * Checks if the current state of the client is valid with a PVCOACC request.
	 */
	private boolean checkPVCOACC() {
		return isAuthent();
	}

	/*
	 * Process a PVCOACC request.
	 */
	private void processPVCOACC() {
		if (arg == 0) {
			if (!checkPVCOACC()) {
				Logger.warning(formatNetworkData(sc, "Client not authenticated"));
				disconnectClient();
				return;
			}
			setReaderUsername();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			readUsernameArg1(reader);
			return;
		case 2:
			readUsernameArg2(reader);
			answerPVCOESTASRC(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOACC");
		}
	}

	/*
	 * Answers by a PVCOESTASRC request and fills the write buffer of the target.
	 */
	private void answerPVCOESTASRC(Username destination) {
		Username source = db.usernameOf(sc).get();
		if (destination.equals(source)) {
			return;
		}

		boolean valid = db.checkPrivateRequest(source, destination);
		Logger.debug(formatNetworkData(sc, "PRIVATE REQUEST ACCEPTATION : " + valid));

		if (!valid) {
			answerERROR(ErrorType.USRNOTPVREQ);
			return;
		}

		ServerSession session = db.sessionOf(destination).get(); // Checked by checkPrivateRequest
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.info(
				formatNetworkRequest(session.sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOESTASRC));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "USERNAME : " + source));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "ADDRESS : " + address));

		if (!ServerCommunication.addRequestPVCOESTASRC(bbTarget, source.toString(), address)) {
			Logger.warning(formatNetworkData(session.sc, "PVCOESTASRC lost : Write Buffer cannot hold it"));
			return;
		}

		session.updateKey();
	}

	/*
	 * Checks if the current state of the client is valid with a PVCOPORT request.
	 */
	private boolean checkPVCOPORT() {
		return isAuthent();
	}

	/*
	 * Process a PVCOPORT request.
	 */
	private void processPVCOPORT() {
		if (arg == 0) {
			if (!checkPVCOPORT()) {
				Logger.warning(formatNetworkData(sc, "Client not authenticated"));
				disconnectClient();
				return;
			}
			setReaderPort();
			return;
		}

		ReaderPort reader = (ReaderPort) serverReader;

		switch (arg) {
		case 1:
			readPortArg1(reader);
			return;
		case 2:
			readPortArg2(reader);
			return;
		case 3:
			readPortArg3(reader);
			return;
		case 4:
			readPortArg4(reader);
			answerPVCOESTADST(reader.username, reader.portMessage, reader.portFile);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOPORT");
		}
	}

	/*
	 * Answers by a PVCOESTADST request and fills the write buffer of the target.
	 */
	private void answerPVCOESTADST(Username source, int portMessage, int portFile) {
		Username destination = db.usernameOf(sc).get();

		boolean valid = db.removePrivateRequest(source, destination);
		Logger.debug(formatNetworkData(sc, "PRIVATE REQUEST ESTABLISHMENT : " + valid));
		if (!valid) {
			Logger.warning(formatNetworkData(sc,
					"Invalid private connection establishment : " + source + " -> " + destination));
			disconnectClient();
			return;
		}

		ServerSession session = db.sessionOf(source).get(); // Checked by removePrivateRequest
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.info(
				formatNetworkRequest(session.sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOESTADST));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "USERNAME : " + destination));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "ADDRESS : " + address));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "PORT MESSAGE : " + portMessage));
		Logger.info(formatNetworkRequest(session.sc, NetworkLogType.WRITE, "PORT FILE : " + portFile));

		if (!ServerCommunication.addRequestPVCOESTADST(bbTarget, destination.toString(), address, portMessage,
				portFile)) {
			Logger.warning(formatNetworkData(session.sc, "PVCOESTADST lost : Write Buffer cannot hold it"));
			return;
		}

		session.updateKey();
	}

	/*
	 * Updates the state of the current session after reading.
	 */
	void updateStateRead() {
		Logger.info(formatNetworkRequest(sc, NetworkLogType.READ, "BUFFER = " + bbRead));
		if (bbRead.hasRemaining()) { // Not finished to read
			return;
		}

		if (protocol == null) { // New request
			int code = processRequestType();
			if (protocol == null) { // Null again => the given code is not valid
				Logger.warning(formatNetworkData(sc, "Invalid protocol code : " + code));
				disconnectClient();
				return;
			}
		}

		// Here : process the request by its type
		switch (protocol) {
		case COREQ:
			processCOREQ();
			return;
		case MSG:
			processMSG();
			return;
		case PVCOREQ:
			processPVCOREQ();
			return;
		case PVCOACC:
			processPVCOACC();
			return;
		case PVCOPORT:
			processPVCOPORT();
			return;
		default:
			Logger.warning(formatNetworkData(sc, "Unsupported protocol request : " + protocol));
			disconnectClient();
			return;
		}

	}

	/*
	 * Computes the new interest ops of the client.
	 */
	private int computeInterestOps() {
		int ops = 0;

		if (bbWrite.position() > 0) { // There is something to write
			ops |= SelectionKey.OP_WRITE;
		}

		if (bbRead.hasRemaining()) { // There is something to read
			ops |= SelectionKey.OP_READ;
		}

		return ops;
	}

	/*
	 * Updates the interest ops of the selection key of the client.
	 */
	void updateKey() {
		if (!key.isValid()) { // It's ok after a call to disconnectClient()
			return;
		}

		int ops = computeInterestOps();
		if (ops == 0) { // Something went wrong
			throw new AssertionError("Key is inactive");
		}
		key.interestOps(ops);
	}

	/*
	 * Disconnects the client.
	 */
	void disconnectClient() {
		Logger.debug(formatNetworkData(sc, "SILENTLY CLOSE"));

		Optional<Username> disconnected = db.removeClient(sc);
		Logger.debug(formatNetworkData(sc, "DISCONNECTION : " + disconnected));

		if (disconnected.isPresent()) {
			String username = disconnected.get().toString();
			Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.DISCONOTIF));
			Logger.info(formatNetworkRequest(sc, NetworkLogType.WRITE, "USERNAME : " + username));

			ByteBuffer bbWriteAll = db.getBroadcastBuffer();
			if (!ServerCommunication.addRequestDISCONOTIF(bbWriteAll, username)) {
				Logger.warning(formatNetworkData(sc, "DISCONOTIF lost : Broadcast Buffer cannot hold it"));
			} else {
				db.updateStateReadAll();
			}

		}

		NetworkCommunication.silentlyClose(sc);
	}

}
