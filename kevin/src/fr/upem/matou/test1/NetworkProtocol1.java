package fr.upem.matou.test1;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.matou.test.NetworkTCP;

public enum NetworkProtocol1 {
    
    /* COREQ */ 	CLIENT_PUBLIC_CONNECTION_REQUEST("COREQ") {
	@Override
	void sendProtocolMessage(SocketChannel sc, Object... args) throws IOException {
	    String pseudo = (String) args[0];
	    NetworkTCP.writeInt(sc, this.ordinal());
	    NetworkTCP.writeStringUTF8(sc, pseudo);
	}

	@Override
	Optional<Object> receiveProtocolMessage(SocketChannel sc) throws IOException {
	    Optional<Integer> optSizePseudo = NetworkTCP.readInt(sc);
	    if(!optSizePseudo.isPresent()) {
		return Optional.empty();
	    }
	    int sizePseudo = optSizePseudo.get();
	    System.out.println("SIZE PSEUDO : " + sizePseudo);
	    
	    Optional<String> optPseudo = NetworkTCP.readStringUTF8(sc, sizePseudo);
	    if(!optPseudo.isPresent()) {
		return Optional.empty();
	    }
	    String pseudo = optPseudo.get();
	    
	    System.out.println("RETURNED PSEUDO : " + pseudo);
	    return Optional.of(pseudo);
	}
    },

    /* MSG */ 		CLIENT_PUBLIC_MESSAGE("MSG") {
	@Override
	void sendProtocolMessage(SocketChannel sc, Object... args) throws IOException {
	    String pseudo = (String) args[0];
	    String message = (String) args[1];
	    
	    NetworkTCP.writeInt(sc, this.ordinal());
	    NetworkTCP.writeStringUTF8(sc, pseudo);
	    NetworkTCP.writeStringUTF8(sc, message);
	}

	@Override
	Optional<Object> receiveProtocolMessage(SocketChannel sc) throws IOException {
	    Optional<Integer> optSizePseudo = NetworkTCP.readInt(sc);
	    if(!optSizePseudo.isPresent()) {
		return Optional.empty();
	    }
	    int sizePseudo = optSizePseudo.get();
	    
	    Optional<String> optPseudo = NetworkTCP.readStringUTF8(sc, sizePseudo);
	    if(!optPseudo.isPresent()) {
		return Optional.empty();
	    }
	    String pseudo = optPseudo.get();
	    
	    Optional<Integer> optSizeMessage = NetworkTCP.readInt(sc);
	    if(!optSizeMessage.isPresent()) {
		return Optional.empty();
	    }
	    int sizeMessage = optSizeMessage.get();
	    
	    Optional<String> optMessage = NetworkTCP.readStringUTF8(sc, sizeMessage);
	    if(!optMessage.isPresent()) {
		return Optional.empty();
	    }
	    String message = optMessage.get();
	    
	    System.out.println("RETURNED PSEUDO : " + pseudo);
	    System.out.println("RETURNED MESSAGE : " + message);
	    return Optional.of("<" + pseudo + "> " + message);
	}
    },
    ;
    
    static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final String code;
    abstract void sendProtocolMessage(SocketChannel sc, Object... args) throws IOException;
    abstract Optional<Object> receiveProtocolMessage(SocketChannel sc) throws IOException;
    
    private NetworkProtocol1(String code) {
	this.code = code;
    }
    
    public static void sendMessage(SocketChannel sc, NetworkProtocol1 protocol, Object... args) throws IOException {
	protocol.sendProtocolMessage(sc,args);
    }
   
    public static Optional<Object> receiveMessage(SocketChannel sc) throws IOException {
	Optional<Integer> optCode = NetworkTCP.readInt(sc);
	if(!optCode.isPresent()) {
	    return Optional.empty();
	}
	int ordinal = optCode.get();
	System.out.println("ORDINAL = " + ordinal);
	return NetworkProtocol1.values()[ordinal].receiveProtocolMessage(sc);
    }
    
    @Override
    public String toString() {
	return "[" + ordinal() + " - " + code + "]";
    }
    
    public static NetworkProtocol1 valueOf (int ordinal) {
	// XXX : tester ordinal
	return values()[ordinal];
    }
    
//    private static ByteBuffer getBytes(ByteBuffer src, int size) {
//	ByteBuffer dst = ByteBuffer.allocate(size);
//	int position = src.position();
//	int oldLimit = src.limit();
//	src.limit(position + size);
//	dst.put(src);
//	src.limit(oldLimit);
//	return dst;
//    }
}
