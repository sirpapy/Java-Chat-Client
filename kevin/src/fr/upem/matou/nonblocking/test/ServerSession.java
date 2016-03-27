package fr.upem.matou.nonblocking.test;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Optional;

class ServerSession {

	private final SocketChannel sc;
	private NetworkProtocol protocol = null;
	private int arg = -1;
	private ByteBuffer bbRead = ByteBuffer.allocate(Integer.BYTES);
	private ByteBuffer bbWrite = ByteBuffer.allocate(0);
	private ClientState clientState = null;

	static interface ClientState {
		// Marker interface
	}

	static class StateCOREQ implements ClientState {
		int sizePseudo;
		String pseudo;
	}

	static class StateMSG implements ClientState {
		int sizeMessage;
		String message;
	}

	ServerSession(SocketChannel sc) {
		this.sc = sc;
	}

	public ByteBuffer getReadBuffer() {
		return bbRead;
	}

	public ByteBuffer getWriteBuffer() {
		return bbWrite;
	}

	private void processRequestType() {
		bbRead.flip();
		int ordinal = bbRead.getInt();
		Optional<NetworkProtocol> optionalProtocol = NetworkProtocol.getProtocol(ordinal);
		if (!optionalProtocol.isPresent()) {
			return;
		}
		protocol = optionalProtocol.get();
		System.out.println("PROTOCOL : " + protocol);
		arg++;
	}

	private void processCOREQinit() {
		bbRead = ByteBuffer.allocate(Integer.BYTES);
		clientState = new StateCOREQ();
		arg++;
	}

	private void processCOREQarg1(StateCOREQ state) {
		bbRead.flip();
		state.sizePseudo = bbRead.getInt();
		System.out.println("SIZE PSEUDO : " + state.sizePseudo);

		bbRead = ByteBuffer.allocate(state.sizePseudo);
		arg++;
	}

	private void processCOREQarg2(StateCOREQ state) {
		bbRead.flip();
		state.pseudo = NetworkServerCommunication.readStringUTF8(bbRead);
		System.out.println("PSEUDO : " + state.pseudo);

		bbRead = ByteBuffer.allocate(Integer.BYTES);
		arg = -1;
		protocol = null;
	}

	private void answerCORES(ServerDataBase db, String pseudo) {
		boolean acceptation = db.addNewClient(sc, pseudo);
		System.out.println("ACCEPTATION : " + acceptation);

		bbWrite = NetworkServerCommunication.encodeRequestCORES(acceptation);
	}

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
		System.out.println("SIZE MESSAGE : " + state.sizeMessage);

		bbRead = ByteBuffer.allocate(state.sizeMessage);
		arg++;
	}

	private void processMSGarg2(StateMSG state) {
		bbRead.flip();
		state.message = NetworkServerCommunication.readStringUTF8(bbRead);
		System.out.println("MESSAGE : " + state.message);

		bbRead = ByteBuffer.allocate(Integer.BYTES);
		arg = -1;
		protocol = null;
	}

	private void answerMSGBC(ServerDataBase db, String message) {
		String pseudo = db.pseudoOf(sc);
		System.out.println("MESSAGE : <" + pseudo + "> " + message);
		
		bbWrite = NetworkServerCommunication.encodeRequestMSGBC(pseudo, message);
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

	void updateStateRead(ServerDataBase db) {
		System.out.println("BUFFER = " + bbRead);
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
		case CLIENT_PUBLIC_CONNECTION_REQUEST:
			processCOREQ(db);
			return;
		case CLIENT_PUBLIC_MESSAGE:
			processMSG(db);
			return;
		default:
			throw new UnsupportedOperationException("Not implemented yet");
		}
	}

	void updateStateWrite(ServerDataBase db) {
		if (bbWrite.position() > 0) { // Not finished to write
			return;
		}
	}

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
