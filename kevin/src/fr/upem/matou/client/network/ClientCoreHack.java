package fr.upem.matou.client.network;

import static fr.upem.matou.client.network.ClientCommunication.*;
import static fr.upem.matou.shared.network.NetworkProtocol.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.NetworkProtocol;
import fr.upem.matou.shared.network.Username;

/*
 * This class is the core of the client.
 */
@SuppressWarnings("javadoc")
public class ClientCoreHack implements Closeable {

	private final SocketChannel sc;

	public ClientCoreHack(String hostname, int port) throws IOException {
		InetSocketAddress address = new InetSocketAddress(hostname, port);
		sc = SocketChannel.open(address);
	}

	@Override
	public void close() throws IOException {
		sc.close();
	}

	private void readServerAnswer() throws IOException {
		NetworkProtocol protocol = ClientCommunication.receiveRequestType(sc);
		System.out.println("PROTOCOL : " + protocol);

		switch (protocol) {

		case ERROR: {
			ErrorType type = ClientCommunication.receiveRequestERROR(sc);
			System.out.println("ERROR : " + type);

			break;
		}

		case CORES: {
			boolean acceptation = ClientCommunication.receiveRequestCORES(sc);
			System.out.println("ACCEPTATION : " + acceptation);
			break;
		}

		case MSGBC: {
			Message message = ClientCommunication.receiveRequestMSGBC(sc);
			System.out.println("USERNAME : " + message.getUsername());
			System.out.println("MESSAGE : " + message.getContent());

			break;
		}

		case CONOTIF: {
			Username connected = ClientCommunication.receiveRequestCONOTIF(sc);
			System.out.println("USERNAME : " + connected);

			break;
		}

		case DISCONOTIF: {
			Username disconnected = ClientCommunication.receiveRequestDISCONOTIF(sc);
			System.out.println("USERNAME : " + disconnected);

			break;
		}

		case PVCOREQNOTIF: {
			Username requester = ClientCommunication.receiveRequestPVCOREQNOTIF(sc);
			System.out.println("USERNAME : " + requester);

			break;
		}

		case PVCOESTASRC: {
			SourceConnectionData sourceInfo = ClientCommunication.receiveRequestPVCOESTASRC(sc);
			Username username = sourceInfo.getUsername();
			InetAddress address = sourceInfo.getAddress();
			System.out.println("USERNAME : " + username);
			System.out.println("ADDRESS : " + address);

			break;
		}

		case PVCOESTADST: {
			DestinationConnectionData destinationInfo = ClientCommunication.receiveRequestPVCOESTADST(sc);
			Username username = destinationInfo.getUsername();
			InetAddress address = destinationInfo.getAddress();
			int portMessage = destinationInfo.getPortMessage();
			int portFile = destinationInfo.getPortFile();
			System.out.println("USERNAME : " + username);
			System.out.println("ADDRESS : " + address);
			System.out.println("PORT MESSAGE : " + portMessage);
			System.out.println("PORT FILE : " + portFile);

			break;
		}

		default:
			throw new IOException("Unsupported protocol request : " + protocol);

		}
	}

	public void hack_MultipleCOREQ() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "abra");
		writeProtocol(sc, COREQ);
		writeString(sc, "kadabra");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_UnauthentMSG() throws IOException {
		writeProtocol(sc, MSG);
		writeString(sc, "Hello World");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_ServerReservedRequest() throws IOException {
		writeProtocol(sc, MSGBC);
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_UsernameEmpty() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_MessageEmpty() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, MSG);
		writeString(sc, "");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_UsernameInvalid() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "Abra Kadabra");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_MessageInvalid() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, MSG);
		writeString(sc, "Testing\n");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_UsernameToLong() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "000000000000000000000000000000000");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_MessageToLong() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, MSG);
		writeString(sc,
				"000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		while (true) {
			readServerAnswer();
		}
	}

	public void hack_UsernameAndMessageFull() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "øøøøøøøøøøøøøøøø");
		writeProtocol(sc, MSG);
		writeString(sc,
				"𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀");
		for (int i = 0; i < 3; i++) {
			readServerAnswer();
		}
	}

	public void hack_FloodMessages() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "øøøøøøøøøøøøøøøø");
		int flood = 100;
		int max = flood + 2;
		for (int i = 0; i < flood; i++) {
			writeProtocol(sc, MSG);
			writeString(sc,
					"𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀𠀀");

		}
		for (int i = 0; i < max; i++) {
			readServerAnswer();
			System.out.println("REQUEST " + (i + 1) + " : OK");
		}
	}

	public void hack_FakePVCOREQ() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, PVCOREQ);
		writeString(sc, "lol");
		for (int i = 0; i < 3; i++) {
			readServerAnswer();
		}
	}
	
	public void hack_FakePVCOACC() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, PVCOACC);
		writeString(sc, "lol");
		for (int i = 0; i < 3; i++) {
			readServerAnswer();
		}
	}

	public void hack_FakePVCOPORT() throws IOException {
		writeProtocol(sc, COREQ);
		writeString(sc, "foo");
		writeProtocol(sc, PVCOPORT);
		writeString(sc, "mdr");
		writeInt(sc, 10);
		writeInt(sc,10);
		while (true) {
			readServerAnswer();
		}
	}

	public void startHack() throws IOException {
		hack_FakePVCOPORT();
	}

}
