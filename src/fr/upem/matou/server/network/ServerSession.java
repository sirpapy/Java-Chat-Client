package fr.upem.matou.server.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.buffer.ByteBuffers;
import fr.upem.matou.logger.Logger;
import fr.upem.matou.logger.Logger.LogType;
import fr.upem.matou.shared.network.NetworkCommunication;
import fr.upem.matou.shared.network.NetworkProtocol;

/*
 * This class represents the state of a client connected to the chat server.
 */
class ServerSession {
	private final ServerDataBase db;
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

	ServerSession(ServerDataBase db, SocketChannel sc) {
		this.db = db;
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
		bbWrite = ByteBuffers.merge(bbWrite, bb);
	}
	
	private void resetReadState() {
		bbRead = ByteBuffer.allocate(Integer.BYTES);
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
		Logger.network(LogType.READ, "PROTOCOL : " + protocol);
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
		Logger.network(LogType.READ, "SIZE PSEUDO : " + state.sizePseudo);

		bbRead = ByteBuffer.allocate(state.sizePseudo);
		arg++;
	}

	/*
	 * Process the read buffer to retrieves the second argument of the COREQ request and updates current state.
	 */
	private void processCOREQarg2(StateCOREQ state) {
		bbRead.flip();
		state.pseudo = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(LogType.READ, "PSEUDO : " + state.pseudo);

		resetReadState();
	}

	/*
	 * Answers by a CORES request and fills the write buffer.
	 */
	private void answerCORES(String pseudo) {
		boolean acceptation = db.addNewClient(sc, pseudo);
		Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.CORES);
		Logger.network(LogType.WRITE, "ACCEPTATION : " + acceptation);

		bbWrite = ServerCommunication.encodeRequestCORES(acceptation);

		if (acceptation) {
			setAuthent();

			Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.CODISP);
			Logger.network(LogType.WRITE, "PSEUDO : " + pseudo);

			ByteBuffer bbWriteAll = ServerCommunication.encodeRequestCODISP(pseudo);
			db.addBroadcast(bbWriteAll);
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

		StateCOREQ state = (StateCOREQ) clientState;

		switch (arg) {
		case 1:
			processCOREQarg1(state);
			return;
		case 2:
			processCOREQarg2(state);
			answerCORES(state.pseudo);
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
		Logger.network(LogType.READ, "SIZE MESSAGE : " + state.sizeMessage);

		// FIXME : Si le message est vide, l'allocation de 0 entrainera la fermeture du client.
		bbRead = ByteBuffer.allocate(state.sizeMessage);
		arg++;
	}

	private void processMSGarg2(StateMSG state) {
		bbRead.flip();
		state.message = ServerCommunication.readStringUTF8(bbRead);
		Logger.network(LogType.READ, "MESSAGE : " + state.message);

		resetReadState();
	}

	private void answerMSGBC(String message) {
		if(!NetworkCommunication.checkMessageValidity(message)) {
			Logger.debug("INVALID MESSAGE : " + message);
			return;
		}
		String pseudo = db.pseudoOf(sc);
		Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.MSGBC);
		Logger.network(LogType.WRITE, "PSEUDO : " + pseudo);
		Logger.network(LogType.WRITE, "MESSAGE : " + message);

		ByteBuffer bbWriteAll = ServerCommunication.encodeRequestMSGBC(pseudo, message);
		db.addBroadcast(bbWriteAll);
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

	private void processDISCOinit() {
		resetReadState();
	}

	private void answerDISCODISP() {
		bbRead = ByteBuffer.allocate(0);
	}

	private void processDISCO() {
		if (arg == 0) {
			processDISCOinit();
			answerDISCODISP();
			return;
		}
		throw new AssertionError("Argument " + arg + " is not valid for DISCO");
	}

	/*
	 * Updates the state of the current session after reading.
	 */
	void updateStateRead() {
		Logger.network(LogType.READ, "BUFFER = " + bbRead);
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
			processCOREQ();
			return;
		case MSG:
			processMSG();
			return;
		case DISCO:
			processDISCO();
			return;
		default:
			throw new UnsupportedOperationException("Not implemented yet");
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
	boolean updateInterestOps(SelectionKey key) {
		if(!key.isValid()) {
			return true;
		}
		
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

	void silentlyClose() {
		Logger.debug("SILENTLY CLOSE OF : " + sc);
		String pseudo = db.removeClient(sc);

		if (pseudo != null) {
			Logger.network(LogType.WRITE, "PROTOCOL : " + NetworkProtocol.DISCODISP);
			Logger.network(LogType.WRITE, "PSEUDO : " + pseudo);
			ByteBuffer bbWriteAll = ServerCommunication.encodeRequestDISCODISP(pseudo);
			db.addBroadcast(bbWriteAll);
			db.updateStateReadAll();
		}

		try {
			sc.close();
		} catch (IOException e) {
			Logger.exception(e);
			return;
		}
	}
}
