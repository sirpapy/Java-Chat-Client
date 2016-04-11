package fr.upem.matou.server.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.debug.buffer.ByteBuffers;
import fr.upem.matou.shared.logger.Logger;
import fr.upem.matou.shared.logger.Logger.NetworkLogType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

// TEMP : Retirer les UnsupportedOperationException !

/*
 * This class represents the state of a client connected to the chat server.
 */
class ServerSession {
	private static final int BUFFER_SIZE_INPUT = NetworkProtocol.getMaxClientToServerRequestSize();
	private static final int BUFFER_SIZE_OUTPUT = NetworkProtocol.getMaxServerToClientRequestSize();
	private static final int USERNAME_MAX_SIZE = NetworkCommunication.getUsernameMaxSize();
	private static final int MESSAGE_MAX_SIZE = NetworkCommunication.getMessageMaxSize();

	private final ServerDataBase db;
	private final SocketChannel sc;
	private final InetAddress address;
	private final SelectionKey key;

	private boolean authent = false;
	private NetworkProtocol protocol = null;
	private int arg = -1;
	private final ByteBuffer bbRead = ByteBuffer.allocateDirect(BUFFER_SIZE_INPUT);
	private final ByteBuffer bbWrite = ByteBuffer.allocateDirect(BUFFER_SIZE_OUTPUT);
	private ClientState clientState = null;

	static interface ClientState {
		// Marker interface
	}

	/* State of a COREQ request */
	static class StateCOREQ implements ClientState {
		int sizeUsername;
		String username;
	}

	/* State of a MSG request */
	static class StateMSG implements ClientState {
		int sizeMessage;
		String message;
	}

	/* State of a PVCOREQ request */
	static class StatePVCOREQ implements ClientState {
		int sizeUsername;
		String username;
	}

	/* State of a PVCOACC request */
	static class StatePVCOACC implements ClientState {
		int sizeUsername;
		String username;
	}

	/* State of a PVCOPORT request */
	static class StatePVCOPORT implements ClientState {
		int sizeUsername;
		String username;
		int sizeAddress;
		InetAddress address;
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

	private void resetReadState() {
		clearAndLimit(bbRead, Integer.BYTES);
		arg = -1;
		protocol = null;
	}

	/*
	 * Retrives the protocol request type from the read buffer and updates current state.
	 */
	private void processRequestType() {
		bbRead.flip();
		int ordinal = bbRead.getInt();
		Optional<NetworkProtocol> optionalProtocol = NetworkProtocol.getProtocol(ordinal);
		if (!optionalProtocol.isPresent()) {
			return;
		}
		protocol = optionalProtocol.get();
		Logger.network(NetworkLogType.READ, "PROTOCOL : " + protocol);
		arg++;
	}

	/*
	 * Process a COREQ request.
	 */
	private void processCOREQ() {
		if (arg == 0) {
			processCOREQinit();
			return;
		}

		StateCOREQ state = (StateCOREQ) clientState;

		switch (arg) {
		case 1:
			processCOREQarg1(state);
			return;
		case 2:
			processCOREQarg2(state);
			answerCORES(state.username);
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
		clearAndLimit(bbRead, Integer.BYTES);
		clientState = new StateCOREQ();
		arg++;
	}

	/*
	 * Process the read buffer to retrieves the first argument of the COREQ request and updates current state.
	 */
	private void processCOREQarg1(StateCOREQ state) {
		bbRead.flip();
		state.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + state.sizeUsername);
		if (state.sizeUsername > USERNAME_MAX_SIZE || state.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeUsername);
		arg++;
	}

	/*
	 * Process the read buffer to retrieves the second argument of the COREQ request and updates current state.
	 */
	private void processCOREQarg2(StateCOREQ state) {
		bbRead.flip();
		state.username = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "USERNAME : " + state.username);

		resetReadState();
	}

	/*
	 * Answers by a CORES request and fills the write buffer.
	 */
	private void answerCORES(String username) {
		if (!NetworkCommunication.checkUsernameValidity(username)) {
			Logger.debug("INVALID USERNAME : " + username);
			disconnectClient();
			return;
		}

		boolean acceptation = db.addNewConnected(sc, new Username(username));
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
			if (!ServerCommunication.addRequestCONOTIF(bbWriteAll, username)) {
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

		StateMSG state = (StateMSG) clientState;

		switch (arg) {
		case 1:
			processMSGarg1(state);
			return;
		case 2:
			processMSGarg2(state);
			answerMSGBC(state.message);
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
		clearAndLimit(bbRead, Integer.BYTES);
		clientState = new StateMSG();
		arg++;
	}

	private void processMSGarg1(StateMSG state) {
		bbRead.flip();
		state.sizeMessage = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE MESSAGE : " + state.sizeMessage);
		if (state.sizeMessage > MESSAGE_MAX_SIZE || state.sizeMessage == 0) {
			Logger.debug("Invalid size message");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeMessage);
		arg++;
	}

	private void processMSGarg2(StateMSG state) {
		bbRead.flip();
		state.message = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "MESSAGE : " + state.message);

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

	private void processDISCO() {
		if (arg == 0) {
			processDISCOinit();
			return;
		}
		throw new AssertionError("Argument " + arg + " is not valid for DISCO");
	}

	private void processDISCOinit() {
		if (!isAuthent()) {
			Logger.debug("Client not authenticated");
		}
		disconnectClient();
	}

	private void processPVCOREQ() {
		if (arg == 0) {
			processPVCOREQinit();
			return;
		}

		StatePVCOREQ state = (StatePVCOREQ) clientState;

		switch (arg) {
		case 1:
			processPVCOREQarg1(state);
			return;
		case 2:
			processPVCOREQarg2(state);
			answerPVCOREQNOTIF(state.username);
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
		clearAndLimit(bbRead, Integer.BYTES);
		clientState = new StatePVCOREQ();
		arg++;
	}

	private void processPVCOREQarg1(StatePVCOREQ state) {
		bbRead.flip();
		state.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + state.sizeUsername);
		if (state.sizeUsername > USERNAME_MAX_SIZE || state.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeUsername);
		arg++;
	}

	private void processPVCOREQarg2(StatePVCOREQ state) {
		bbRead.flip();
		state.username = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "USERNAME : " + state.username);

		resetReadState();
	}

	// FIXME : target == source
	private void answerPVCOREQNOTIF(String targetName) {
		Username source = db.usernameOf(sc).get();
		Username target = new Username(targetName);
		Optional<ServerSession> optional = db.sessionOf(target);
		if (!optional.isPresent()) {
			Logger.debug("Target " + target + " is not connected");
			return;
		}
		db.addNewPrivateRequest(source, target); // TEMP : Username en argument
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

		StatePVCOACC state = (StatePVCOACC) clientState;

		switch (arg) {
		case 1:
			processPVCOACCarg1(state);
			return;
		case 2:
			processPVCOACCarg2(state);
			answerPVCOESTASRC(state.username);
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
		clearAndLimit(bbRead, Integer.BYTES);
		clientState = new StatePVCOACC();
		arg++;
	}

	private void processPVCOACCarg1(StatePVCOACC state) {
		bbRead.flip();
		state.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + state.sizeUsername);
		if (state.sizeUsername > USERNAME_MAX_SIZE || state.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeUsername);
		arg++;
	}

	private void processPVCOACCarg2(StatePVCOACC state) {
		bbRead.flip();
		state.username = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "USERNAME : " + state.username);

		resetReadState();
	}

	private void answerPVCOESTASRC(String targetName) {
		Username source = db.usernameOf(sc).get();
		Username target = new Username(targetName);
		boolean valid = db.checkPrivateRequest(source, target); // TEMP : Username en argument
		Logger.debug("REQUEST ACCEPTATION : " + valid);

		if (valid) {
			Optional<ServerSession> optional = db.sessionOf(target);
			if (!optional.isPresent()) { // XXX : Impossible car checked
				Logger.debug("Target " + target + " is not connected");
				return;
			}

			ServerSession session = optional.get();
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
	}

	private void processPVCOPORT() {
		if (arg == 0) {
			processPVCOPORTinit();
			return;
		}

		StatePVCOPORT state = (StatePVCOPORT) clientState;

		switch (arg) {
		case 1:
			processPVCOPORTarg1(state);
			return;
		case 2:
			processPVCOPORTarg2(state);
			break;
		case 3:
			processPVCOPORTarg3(state);
			break;
		case 4:
			processPVCOPORTarg4(state);
			break;
		case 5:
			processPVCOPORTarg5(state);
			break;
		case 6:
			processPVCOPORTarg6(state);
			answerPVCOESTADST(state.username, state.address, state.portMessage, state.portFile);
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
		// FIXME : Check accepted
		clearAndLimit(bbRead, Integer.BYTES);
		clientState = new StatePVCOPORT();
		arg++;
	}

	private void processPVCOPORTarg1(StatePVCOPORT state) {
		bbRead.flip();
		state.sizeUsername = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE USERNAME : " + state.sizeUsername);
		if (state.sizeUsername > USERNAME_MAX_SIZE || state.sizeUsername == 0) {
			Logger.debug("Invalid size username");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeUsername);
		arg++;
	}

	private void processPVCOPORTarg2(StatePVCOPORT state) {
		bbRead.flip();
		state.username = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(NetworkLogType.READ, "USERNAME : " + state.username);

		clearAndLimit(bbRead, Integer.BYTES);
		arg++;
	}

	private void processPVCOPORTarg3(StatePVCOPORT state) {
		bbRead.flip();
		state.sizeAddress = bbRead.getInt();
		Logger.network(NetworkLogType.READ, "SIZE ADDRESS : " + state.sizeAddress);
		if (state.sizeAddress != 4 && state.sizeAddress != 16) {
			Logger.debug("Invalid size address");
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, state.sizeAddress);
		arg++;
	}

	private void processPVCOPORTarg4(StatePVCOPORT state) {
		bbRead.flip();
		byte[] addr = new byte[state.sizeAddress];
		for (int i = 0; i < state.sizeAddress; i++) {
			byte b = bbRead.get();
			System.out.println("Byte : " + b);
			addr[i] = b;
		}
		try {
			state.address = InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			Logger.exception(e);
			disconnectClient();
			return;
		}

		clearAndLimit(bbRead, Integer.BYTES);
		arg++;
	}

	private void processPVCOPORTarg5(StatePVCOPORT state) {
		bbRead.flip();
		state.portMessage = bbRead.getInt();

		clearAndLimit(bbRead, Integer.BYTES);
		arg++;
	}

	private void processPVCOPORTarg6(StatePVCOPORT state) {
		bbRead.flip();
		state.portFile = bbRead.getInt();

		resetReadState();
	}

	private void answerPVCOESTADST(String targetName, InetAddress targetAddress, int portMessage, int portFile) {
		Username source = db.usernameOf(sc).get();
		Username target = new Username(targetName);
		boolean valid = db.checkPrivateRequest(target, source); // TEMP : Username en argument
		Logger.debug("PRIVATE ESTABLISHMENT VALIDITY : " + valid); // FIXME : False

		if (valid) {
			Optional<ServerSession> optional = db.sessionOf(target);
			if (!optional.isPresent()) { // XXX : Impossible car checked
				Logger.debug("Target " + target + " is not connected");
				return;
			}

			ServerSession session = optional.get();
			ByteBuffer bbTarget = session.getWriteBuffer();
			Logger.network(NetworkLogType.WRITE, "PROTOCOL : " + NetworkProtocol.PVCOESTADST);
			Logger.network(NetworkLogType.WRITE, "USERNAME : " + source);
			Logger.network(NetworkLogType.WRITE, "ADDRESS : " + targetAddress);
			Logger.network(NetworkLogType.WRITE, "PORT MESSAGE : " + portMessage );
			Logger.network(NetworkLogType.WRITE, "PORT FILE : " + portFile);

			if (!ServerCommunication.addRequestPVCOESTADST(bbTarget, source.toString(), targetAddress, portMessage, portFile)) {
				Logger.warning("PVCOESTADST lost | Write Buffer cannot hold it");
				return;
			}

			session.updateKey();
		} 
	}

	/*
	 * Updates the state of the current session after reading.
	 */
	void updateStateRead() {
		Logger.network(NetworkLogType.READ, "BUFFER = " + bbRead);
		if (bbRead.hasRemaining()) { // Not finished to read
			return;
		}

		if (protocol == null) {
			processRequestType();
		}

		if (arg == -1) {
			Logger.warning("ARG UNSET BUFFER : " + bbRead);
			return;
		}

		switch (protocol) {
		case COREQ:
			processCOREQ();
			return;
		case MSG:
			processMSG();
			return;
		case DISCO:
			processDISCO();
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
			Logger.error("Operation not implemented yet : " + protocol); // TEMP
			disconnectClient();
			return;
		}

	}

	/*
	 * Updates the state of the current session after writing.
	 */
	void updateStateWrite() {
		if (bbWrite.position() > 0) { // Not finished to write
			return;
		}
		// TEMP
	}

	/*
	 * Updates the interest operations after reading or writing.
	 */
	int computeInterestOps() {
		// if (!key.isValid()) {
		// return true;
		// }
		int ops = 0;

		if (bbWrite.position() > 0) { // There is something to write
			ops |= SelectionKey.OP_WRITE;
		}

		if (bbRead.hasRemaining()) { // There is something to read
			ops |= SelectionKey.OP_READ;
		}

		return ops;
	}

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

		silentlyClose(sc);
	}

	private static void silentlyClose(SocketChannel sc) {
		try {
			sc.close();
		} catch (IOException e) {
			Logger.exception(e);
			return;
		}
	}

	void updateKey() {
		if (!key.isValid()) {
			Logger.debug("Key not valid anymore");
			return;
		}

		int ops = computeInterestOps();
		if (ops == 0) {
			throw new AssertionError("Key is inactive");
		}
		key.interestOps(ops);
	}
}
