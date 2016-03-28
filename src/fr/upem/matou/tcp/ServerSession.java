package fr.upem.matou.tcp;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.buffer.ByteBuffers;
import fr.upem.matou.logger.Logger;
import fr.upem.matou.logger.Logger.LogType;

/*
 * This class represents the state of a client connected to the chat server.
 */
class ServerSession {
	private final SocketChannel sc;
	private boolean authent = false;
	private NetworkProtocol protocol = null;
	private int arg = -1;
	private ByteBuffer bbRead = ByteBuffer.allocate(Integer.BYTES);
	private ByteBuffer bbWrite = ByteBuffer.allocate(0);
	private ClientState clientState = null;

	static interface ClientState {
		// Marker interface
	}

	/* State of a COREQ request */
	static class StateCOREQ implements ClientState {
		int sizePseudo;
		String pseudo;
	}

	/* State of a MSG request */
	static class StateMSG implements ClientState {
		int sizeMessage;
		String message;
	}

	ServerSession(SocketChannel sc) {
		this.sc = sc;
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

	void appendWriteBuffer(ByteBuffer bb) {
		bbWrite = ByteBuffers.merge(bbWrite,bb);
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
		Logger.network(LogType.READ,"PROTOCOL : " + protocol);
		arg++;
	}

	/*
	 * Initializes the COREQ state.
	 */
	private void processCOREQinit() {
		bbRead = ByteBuffer.allocate(Integer.BYTES);
		clientState = new StateCOREQ();
		arg++;
	}

	/*
	 * Process the read buffer to retrieves the first argument of the COREQ request and updates current state.
	 */
	private void processCOREQarg1(StateCOREQ state) {
		bbRead.flip();
		state.sizePseudo = bbRead.getInt();
		Logger.network(LogType.READ,"SIZE PSEUDO : " + state.sizePseudo);

		bbRead = ByteBuffer.allocate(state.sizePseudo);
		arg++;
	}

	/*
	 * Process the read buffer to retrieves the second argument of the COREQ request and updates current state.
	 */
	private void processCOREQarg2(StateCOREQ state) {
		bbRead.flip();
		state.pseudo = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(LogType.READ,"PSEUDO : " + state.pseudo);

		bbRead = ByteBuffer.allocate(Integer.BYTES);
		arg = -1;
		protocol = null;
	}

	/*
	 * Answers by a CORES request and fills the write buffer.
	 */
	private void answerCORES(ServerDataBase db, String pseudo) {
		boolean acceptation = db.addNewClient(sc, pseudo);
		Logger.network(LogType.WRITE,"PROTOCOL : " + NetworkProtocol.CORES);
		Logger.network(LogType.WRITE,"ACCEPTATION : " + acceptation);

		if(acceptation) {
			setAuthent();
		}
		
		bbWrite = ServerCommunication.encodeRequestCORES(acceptation);
	}

	/*
	 * Process a COREQ request.
	 */
	private void processCOREQ(ServerDataBase db) {
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
			answerCORES(db, state.pseudo);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for COREQ");
		}
	}

	private void processMSGinit() {
		bbRead = ByteBuffer.allocate(Integer.BYTES);
		clientState = new StateMSG();
		arg++;
	}

	private void processMSGarg1(StateMSG state) {
		bbRead.flip();
		state.sizeMessage = bbRead.getInt();
		Logger.network(LogType.READ,"SIZE MESSAGE : " + state.sizeMessage);

		bbRead = ByteBuffer.allocate(state.sizeMessage);
		arg++;
	}

	private void processMSGarg2(StateMSG state) {
		bbRead.flip();
		state.message = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(LogType.READ,"MESSAGE : " + state.message);

		bbRead = ByteBuffer.allocate(Integer.BYTES);
		arg = -1;
		protocol = null;
	}

	private void answerMSGBC(ServerDataBase db, String message) {
		String pseudo = db.pseudoOf(sc);
		Logger.network(LogType.WRITE,"PROTOCOL : " + NetworkProtocol.MSGBC);
		Logger.network(LogType.WRITE,"PSEUDO : " + pseudo);
		Logger.network(LogType.WRITE,"MESSAGE : " + message);

		ByteBuffer bbWriteAll = ServerCommunication.encodeRequestMSGBC(pseudo, message);
		db.addBroadcast(bbWriteAll);
	}

	private void processMSG(ServerDataBase db) {
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
			answerMSGBC(db, state.message);
			return;
		default:
			throw new AssertionError("Argument " + arg + " is not valid for COREQ");
		}
	}

	/*
	 * Updates the state of the current session after reading.
	 */
	void updateStateRead(ServerDataBase db) {
		Logger.network(LogType.READ,"BUFFER = " + bbRead);
		if (bbRead.hasRemaining()) { // Not finished to read
			return;
		}

		if (protocol == null) {
			processRequestType();
		}

		if (arg == -1) {
			return;
		}

		switch (protocol) {
		case COREQ:
			processCOREQ(db);
			return;
		case MSG:
			processMSG(db);
			return;
		default:
			throw new UnsupportedOperationException("Not implemented yet");
		}

	}

	/*
	 * Updates the state of the current session after writing.
	 */
	void updateStateWrite(ServerDataBase db) {
		if (bbWrite.position() > 0) { // Not finished to write
			return;
		}
	}

	/*
	 * Updates the interest operations after reading or writing.
	 */
	boolean updateInterestOps(SelectionKey key) {
		int ops = 0;

		if (bbWrite.position() > 0) { // There is something to write
			ops |= SelectionKey.OP_WRITE;
		}

		if (bbRead.hasRemaining()) { // There is something to read
			ops |= SelectionKey.OP_READ;
		}

		key.interestOps(ops);
		return ops != 0;
	}

}
