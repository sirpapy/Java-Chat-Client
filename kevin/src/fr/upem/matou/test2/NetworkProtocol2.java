package fr.upem.matou.test2;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import fr.upem.matou.test.NetworkTCP;

public enum NetworkProtocol2 {
    /* COREQ */ CLIENT_PUBLIC_CONNECTION_REQUEST("COREQ"),
    /* MSG */ CLIENT_PUBLIC_MESSAGE("MSG"),;

    private final String code;

    private NetworkProtocol2(String code) {
	this.code = code;
    }

    @Override
    public String toString() {
	return "[" + ordinal() + " - " + code + "]";
    }
    
    public static Optional<NetworkProtocol2> receiveProtocolRequest(SocketChannel sc) throws IOException {
	Optional<Integer> optCode = NetworkTCP.readInt(sc);
	if(!optCode.isPresent()) {
	    return Optional.empty();
	}
	int ordinal = optCode.get();
	return Optional.of(valueOf(ordinal));
    }
    
    public static NetworkProtocol2 valueOf (int ordinal) {
	// XXX : tester ordinal
	return values()[ordinal];
    }
    
    public static void sendRequestCOREQ(SocketChannel sc, String pseudo) throws IOException {
	NetworkTCP.writeInt(sc, CLIENT_PUBLIC_CONNECTION_REQUEST.ordinal());
	NetworkTCP.writeStringUTF8(sc, pseudo);
    }

    public static Optional<String> receiveRequestCOREQ(SocketChannel sc) throws IOException {
	Optional<Integer> optSizePseudo = NetworkTCP.readInt(sc);
	if (!optSizePseudo.isPresent()) {
	    return Optional.empty();
	}
	int sizePseudo = optSizePseudo.get();
	System.out.println("SIZE PSEUDO : " + sizePseudo);

	Optional<String> optPseudo = NetworkTCP.readStringUTF8(sc, sizePseudo);
	if (!optPseudo.isPresent()) {
	    return Optional.empty();
	}
	String pseudo = optPseudo.get();

	System.out.println("RETURNED PSEUDO : " + pseudo);
	return Optional.of(pseudo);
    }

    public static void sendRequestMSG(SocketChannel sc, String pseudo, String message) throws IOException {
	NetworkTCP.writeInt(sc, CLIENT_PUBLIC_MESSAGE.ordinal());
	NetworkTCP.writeStringUTF8(sc, pseudo);
	NetworkTCP.writeStringUTF8(sc, message);
    }

    public static Optional<String> receiveRequestMSG(SocketChannel sc) throws IOException {
	Optional<Integer> optSizePseudo = NetworkTCP.readInt(sc);
	if (!optSizePseudo.isPresent()) {
	    return Optional.empty();
	}
	int sizePseudo = optSizePseudo.get();

	Optional<String> optPseudo = NetworkTCP.readStringUTF8(sc, sizePseudo);
	if (!optPseudo.isPresent()) {
	    return Optional.empty();
	}
	String pseudo = optPseudo.get();

	Optional<Integer> optSizeMessage = NetworkTCP.readInt(sc);
	if (!optSizeMessage.isPresent()) {
	    return Optional.empty();
	}
	int sizeMessage = optSizeMessage.get();

	Optional<String> optMessage = NetworkTCP.readStringUTF8(sc, sizeMessage);
	if (!optMessage.isPresent()) {
	    return Optional.empty();
	}
	String message = optMessage.get();

	System.out.println("RETURNED PSEUDO : " + pseudo);
	System.out.println("RETURNED MESSAGE : " + message);
	return Optional.of("<" + pseudo + "> " + message);
    }

}
