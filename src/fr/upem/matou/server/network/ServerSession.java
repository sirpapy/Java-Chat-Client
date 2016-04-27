package fr.upem.matou.server.network;

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
 * This class represents the state of a client connected to the chat server.
 * A ServerSession is always attached to one ServerDataBase and should be created by
 * ServerDataBase.newServerSession(SocketChannel,SelectionKey).
 */
class ServerSession {

	private static final int BUFFER_SIZE_INPUT = NetworkProtocol.getServerReadBufferSize();
	private static final int BUFFER_SIZE_OUTPUT = NetworkProtocol.getServerWriteBufferSize(); // FIXME : Congestion
	private static final int USERNAME_MAX_SIZE = NetworkCommunication.getUsernameMaxSize();
	private static final int MESSAGE_MAX_SIZE = NetworkCommunication.getMessageMaxSize();

	private final ServerDataBase db;
	private final SocketChannel sc;
	private final InetAddress address; // FIXME : Localhost
	private final SelectionKey key;

	private final ByteBuffer bbRead = ByteBuffer.allocateDirect(BUFFER_SIZE_INPUT);
	private final ByteBuffer bbWrite = ByteBuffer.allocateDirect(BUFFER_SIZE_OUTPUT);

	private boolean authent = false; // If the client has a username
	private NetworkProtocol protocol = null;
	private int arg = 0;
	private ServerReader serverReader = null;

	static interface ServerReader {
		// Marker interface
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
		clearAndLimit(bbRead, Integer.BYTES);
	}

	boolean isAuthent() {
		return authent;
	}

	void setAuthent() {
		authent = true;
	}

	ByteBuffer getReadBuffer() {
		return bbRead;
	}

	ByteBuffer getWriteBuffer() {
		return bbWrite;
	}

	private final static void clearAndLimit(ByteBuffer bb, int size) {
		bb.clear();
		bb.limit(size);
	}

	void appendWriteBuffer(ByteBuffer bb) {
		boolean succeeded = ByteBuffers.append(bbWrite, bb);
		if (!succeeded) {
			Logger.warning("REQUEST LOST | WRITE BUFFER CANNOT HOLD IT");
		}
	}

	/*
	 * Resets the read state of this client in order to read another request.
	 */
	private void resetReadState() {
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
	 * Retrives the protocol request type from the read buffer and updates current state.
	 */
	private int processRequestType() {
		bbRead.flip();
		int ordinal = bbRead.getInt();
		Optional<NetworkProtocol> optionalProtocol = NetworkProtocol.getProtocol(ordinal);
		if (!optionalProtocol.isPresent()) {
			return ordinal;
		}
		protocol = optionalProtocol.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);
		return ordinal;
	}

	private void answerERROR(ErrorType type) {
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.ERROR);
		Logger.network(NetworkLogType.WRITE, "ERROR : " + type);
		
		if(!ServerCommunication.addRequestERROR(bbWrite, type)) {
			Logger.warning("ERROR lost | Write Buffer cannot hold it");
			return;
		}
	}
	
	/*
	 * Process a COREQ request.
	 */
	private void processCOREQ() {
		if (arg == 0) {
			processCOREQinit();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			processCOREQarg1(reader);
			return;
		case 2:
			processCOREQarg2(reader);
			answerCORES(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for COREQ");
		}
	}

	/*
	 * Initializes the COREQ state.
	 */
	private void processCOREQinit() {
		if (isAuthent()) {
			Logger.debug("Client already authenticated");
			disconnectClient();
			return;
		}
		serverReader = new ReaderUsername();
		advanceReadState(Integer.BYTES);
	}

	/*
	 * Process the read buffer to retrieves the first argument of the COREQ request and updates the current state.
	 */
	private void processCOREQarg1(ReaderUsername reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername);
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	/*
	 * Process the read buffer to retrieves the second argument of the COREQ request and updates the current state.
	 */
	private void processCOREQarg2(ReaderUsername reader) {
		bbRead.flip();
		String string = ServerCommunication.readStringUTF8(bbRead);
		reader.username = new Username(string);
		Logger.network(NetworkLogType.READ, "USERNAME : " + reader.username);

		resetReadState();
	}

	/*
	 * Answers by a CORES request and fills the write buffer.
	 */
	private void answerCORES(Username username) {
		if (!NetworkCommunication.checkUsernameValidity(username.toString())) {
			Logger.debug("INVALID USERNAME : " + username);
			disconnectClient();
			return;
		}

		boolean acceptation = db.authentClient(sc, username);
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.CORES);
		Logger.network(NetworkLogType.WRITE, "ACCEPTATION : " + acceptation);

		if (!ServerCommunication.addRequestCORES(bbWrite, acceptation)) {
			Logger.warning("CORES lost | Write Buffer cannot hold it");
		}

		if (acceptation) {
			setAuthent();

			Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.CONOTIF);
			Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);

			ByteBuffer bbWriteAll = db.getBroadcastBuffer();
			if (!ServerCommunication.addRequestCONOTIF(bbWriteAll, username.toString())) {
				Logger.warning("CONOTIF lost | Broadcast Buffer cannot hold it");
				return;
			}
			db.updateStateReadAll();
		}
	}

	private void processMSG() {
		if (arg == 0) {
			processMSGinit();
			return;
		}

		ReaderMessage reader = (ReaderMessage) serverReader;

		switch (arg) {
		case 1:
			processMSGarg1(reader);
			return;
		case 2:
			processMSGarg2(reader);
			answerMSGBC(reader.message);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for MSG");
		}
	}

	private void processMSGinit() {
		if (!isAuthent()) {
			Logger.debug("Client not authenticated");
			disconnectClient();
			return;
		}

		serverReader = new ReaderMessage();
		advanceReadState(Integer.BYTES);
	}

	private void processMSGarg1(ReaderMessage reader) {
		bbRead.flip();
		reader.sizeMessage = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE MESSAGE : " + reader.sizeMessage);
		if (reader.sizeMessage > MESSAGE_MAX_SIZE || reader.sizeMessage == 0) {
			Logger.debug("Invalid size message");
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeMessage);
	}

	private void processMSGarg2(ReaderMessage reader) {
		bbRead.flip();
		reader.message = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "MESSAGE : " + reader.message);

		resetReadState();
	}

	private void answerMSGBC(String message) {
		if (!NetworkCommunication.checkMessageValidity(message)) {
			Logger.debug("INVALID MESSAGE : " + message);
			disconnectClient();
			return;
		}
		Username username = db.usernameOf(sc).get();
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSGBC);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
		Logger.network(NetworkLogType.WRITE, "MESSAGE : " + message);

		ByteBuffer bbWriteAll = db.getBroadcastBuffer();
		if (!ServerCommunication.addRequestMSGBC(bbWriteAll, username.toString(), message)) {
			Logger.warning("MSGBC lost | Broadcast Buffer cannot hold it");
			return;
		}
		db.updateStateReadAll();
	}

	private void processPVCOREQ() {
		if (arg == 0) {
			processPVCOREQinit();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			processPVCOREQarg1(reader);
			return;
		case 2:
			processPVCOREQarg2(reader);
			answerPVCOREQNOTIF(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOREQ");
		}
	}

	private void processPVCOREQinit() {
		if (!isAuthent()) {
			Logger.debug("Client not authenticated");
			disconnectClient();
			return;
		}

		serverReader = new ReaderUsername();
		advanceReadState(Integer.BYTES);
	}

	private void processPVCOREQarg1(ReaderUsername reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername);
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	private void processPVCOREQarg2(ReaderUsername reader) {
		bbRead.flip();
		String string = ServerCommunication.readStringUTF8(bbRead);
		reader.username = new Username(string);
		Logger.network(NetworkLogType.READ, "USERNAME : " + reader.username);

		resetReadState();
	}

	private void answerPVCOREQNOTIF(Username reader) {
		// TEMP : Cas où "target == source"
		Username source = db.usernameOf(sc).get();
		Optional<ServerSession> optional = db.sessionOf(reader);
		if (!optional.isPresent()) {
			Logger.debug("Target " + reader + " is not connected");
			answerERROR(ErrorType.USRNOTCO);
			return;
		}

		boolean valid = db.addNewPrivateRequest(source, reader);
		Logger.debug("PRIVATE REQUEST VALIDITY : " + valid);
		if (!valid) {
			return;
		}

		ServerSession session = optional.get();
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOREQNOTIF);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + source);

		if (!ServerCommunication.addRequestPVCOREQNOTIF(bbTarget, source.toString())) {
			Logger.warning("PVCOREQNOTIF lost | Write Buffer cannot hold it");
			return;
		}

		session.updateKey();
	}

	private void processPVCOACC() {
		if (arg == 0) {
			processPVCOACCinit();
			return;
		}

		ReaderUsername reader = (ReaderUsername) serverReader;

		switch (arg) {
		case 1:
			processPVCOACCarg1(reader);
			return;
		case 2:
			processPVCOACCarg2(reader);
			answerPVCOESTASRC(reader.username);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOACC");
		}
	}

	private void processPVCOACCinit() {
		if (!isAuthent()) {
			Logger.debug("Client not authenticated");
			disconnectClient();
			return;
		}

		serverReader = new ReaderUsername();
		advanceReadState(Integer.BYTES);
	}

	private void processPVCOACCarg1(ReaderUsername reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername);
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	private void processPVCOACCarg2(ReaderUsername reader) {
		bbRead.flip();
		String string = ServerCommunication.readStringUTF8(bbRead);
		reader.username = new Username(string);
		Logger.network(NetworkLogType.READ, "USERNAME : " + reader.username);

		resetReadState();
	}

	private void answerPVCOESTASRC(Username target) {
		Username source = db.usernameOf(sc).get();
		boolean valid = db.checkPrivateRequest(source, target);
		Logger.debug("PRIVATE REQUEST ACCEPTATION : " + valid);

		if (!valid) {
			answerERROR(ErrorType.USRNOTPVREQ);
			return;
		}

		ServerSession session = db.sessionOf(target).get(); // Checked by checkPrivateRequest
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOESTASRC);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + source);
		Logger.network(NetworkLogType.WRITE, "ADDRESS : " + address);

		if (!ServerCommunication.addRequestPVCOESTASRC(bbTarget, source.toString(), address)) {
			Logger.warning("PVCOESTASRC lost | Write Buffer cannot hold it");
			return;
		}

		session.updateKey();

	}

	private void processPVCOPORT() {
		if (arg == 0) {
			processPVCOPORTinit();
			return;
		}

		ReaderPort reader = (ReaderPort) serverReader;

		switch (arg) {
		case 1:
			processPVCOPORTarg1(reader);
			return;
		case 2:
			processPVCOPORTarg2(reader);
			return;
		case 3:
			processPVCOPORTarg3(reader);
			return;
		case 4:
			processPVCOPORTarg4(reader);
			answerPVCOESTADST(reader.username, reader.portMessage, reader.portFile);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for PVCOPORT");
		}
	}

	private void processPVCOPORTinit() {
		if (!isAuthent()) {
			Logger.debug("Client not authenticated");
			disconnectClient();
			return;
		}

		serverReader = new ReaderPort();
		advanceReadState(Integer.BYTES);
	}

	private void processPVCOPORTarg1(ReaderPort reader) {
		bbRead.flip();
		reader.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + reader.sizeUsername);
		if (reader.sizeUsername > USERNAME_MAX_SIZE || reader.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		advanceReadState(reader.sizeUsername);
	}

	private void processPVCOPORTarg2(ReaderPort reader) {
		bbRead.flip();
		String string = ServerCommunication.readStringUTF8(bbRead);
		reader.username = new Username(string);
		Logger.network(NetworkLogType.READ, "USERNAME : " + reader.username);

		advanceReadState(Integer.BYTES);
	}

	private void processPVCOPORTarg3(ReaderPort reader) {
		bbRead.flip();
		reader.portMessage = bbRead.getInt();

		advanceReadState(Integer.BYTES);
	}

	private void processPVCOPORTarg4(ReaderPort reader) {
		bbRead.flip();
		reader.portFile = bbRead.getInt();

		resetReadState();
	}

	private void answerPVCOESTADST(Username source, int portMessage, int portFile) {
		Username target = db.usernameOf(sc).get();
		boolean valid = db.removePrivateRequest(source, target);
		Logger.debug("PRIVATE REQUEST ESTABLISHMENT : " + valid);

		if (!valid) {
			Logger.warning("Invalid private connection establishment of " + source + " to " + target);
			disconnectClient();
			return;
		}

		ServerSession session = db.sessionOf(source).get(); // Checked by removePrivateRequest
		ByteBuffer bbTarget = session.getWriteBuffer();
		Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOESTADST);
		Logger.network(NetworkLogType.WRITE, "USERNAME : " + target);
		Logger.network(NetworkLogType.WRITE, "ADDRESS : " + address);
		Logger.network(NetworkLogType.WRITE, "PORT MESSAGE : " + portMessage);
		Logger.network(NetworkLogType.WRITE, "PORT FILE : " + portFile);

		if (!ServerCommunication.addRequestPVCOESTADST(bbTarget, target.toString(), address, portMessage, portFile)) {
			Logger.warning("PVCOESTADST lost | Write Buffer cannot hold it");
			return;
		}

		session.updateKey();
	}

	/*
	 * Updates the state of the current session after reading.
	 */
	void updateStateRead() {
		Logger.network(NetworkLogType.READ, "BUFFER = " + bbRead);
		if (bbRead.hasRemaining()) { // Not finished to read
			return;
		}

		if (protocol == null) { // New request
			int code = processRequestType();
			if (protocol == null) {
				Logger.warning("Invalid protocol code : " + code);
				disconnectClient();
				return;
			}
		}

		// Here : process the request's arguments.		
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
			Logger.warning("Operation not implemented : " + protocol); // TEMP
			disconnectClient();
			return;
		}

	}

	/*
	 * Computes the new interest ops of the client.
	 */
	int computeInterestOps() {
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
		if (!key.isValid()) {
			Logger.debug("This key is not valid anymore");
			return;
		}

		int ops = computeInterestOps();
		if (ops == 0) {
			throw new AssertionError("Key is inactive"); // TEMP for debug
		}
		key.interestOps(ops);
	}

	/*
	 * Disconnects the client.
	 */
	void disconnectClient() {
		Logger.debug("SILENTLY CLOSE OF : " + sc);
		Optional<Username> disconnected = db.removeClient(sc);
		if (!disconnected.isPresent()) {
			Logger.debug("DISCONNECTION : {UNAUTHENTICATED CLIENT}");
		} else {
			Logger.debug("DISCONNECTION : " + disconnected);
			String username = disconnected.get().toString();
			Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.DISCONOTIF);
			Logger.network(NetworkLogType.WRITE, "USERNAME : " + username);
			ByteBuffer bbWriteAll = db.getBroadcastBuffer();
			if (!ServerCommunication.addRequestDISCONOTIF(bbWriteAll, username)) {
				Logger.warning("DISCONOTIF lost | Broadcast Buffer cannot hold it");
			} else {
				db.updateStateReadAll();
			}
		}

		NetworkCommunication.silentlyClose(sc);
	}

}
