package fr.upem.matou.trash;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public enum TestNetworkProtocol1 {
    
    /* COREQ */ 	CLIENT_PUBLIC_CONNECTION_REQUEST("COREQ") {
	@Override
	ByteBuffer encodeProtocolMessage(Object[] args) {
	    String pseudo = (String) args[0];
	    
	    ByteBuffer encodedPseudo = UTF8_CHARSET.encode(pseudo);
	    
	    int sizePseudo = encodedPseudo.remaining();
	    
	    int capacity = Integer.BYTES + sizePseudo;
	    ByteBuffer bb = ByteBuffer.allocate(capacity);
	    bb.putInt(sizePseudo);
	    bb.put(encodedPseudo);
	    
	    return bb;
	}

	@Override
	Object decodeProtocolMessage(ByteBuffer bb) {
	    bb.flip();
	    
	    int sizePseudo = bb.getInt();
	    ByteBuffer encodedPseudo = getBytes(bb, sizePseudo);
	    
	    String pseudo = UTF8_CHARSET.decode(encodedPseudo).toString();
	    
	    return pseudo;
	}
    },
//    /* CORES */ 	SERVER_PUBLIC_CONNECTION_RESPONSE("CORES"),
//    /* CODISP */ 	SERVER_PUBLIC_CONNECTION_NOTIFICATION("CODISP"),
    /* MSG */ 		CLIENT_PUBLIC_MESSAGE("MSG") {
	@Override
	ByteBuffer encodeProtocolMessage(Object[] args) {
	    String pseudo = (String) args[0];
	    String message = (String) args[1];
	    
	    ByteBuffer encodedPseudo = UTF8_CHARSET.encode(pseudo);
	    ByteBuffer encodedMessage = UTF8_CHARSET.encode(message);
	    
	    int sizePseudo = encodedPseudo.remaining();
	    int sizeMessage = encodedMessage.remaining();
	    
	    int capacity = 2 * Integer.BYTES + sizePseudo + sizeMessage;
	    ByteBuffer bb = ByteBuffer.allocate(capacity);
	    bb.putInt(sizePseudo);
	    bb.put(encodedPseudo);
	    bb.putInt(sizeMessage);
	    bb.put(encodedMessage);
	    
	    return bb;
	}

	@Override
	Object decodeProtocolMessage(ByteBuffer bb) {
	    bb.flip();
	    
	    int sizePseudo = bb.getInt();
	    ByteBuffer encodedPseudo = getBytes(bb, sizePseudo);
	    int sizeMessage = bb.getInt();
	    ByteBuffer encodedMessage = getBytes(bb, sizeMessage);
	    
	    String pseudo = UTF8_CHARSET.decode(encodedPseudo).toString();
	    String message = UTF8_CHARSET.decode(encodedMessage).toString();
	    
	    return "<" + pseudo + "> " + message;
	}
    },
//    /* MSGBC */ 	SERVER_PUBLIC_MESSAGE_BROADCAST("MSGBC"),
//    /* PVCOREQ */ 	CLIENT_PRIVATE_CONNECTION_REQUEST("PVCOREQ"),
//    /* PVCOTR */ 	SERVER_PRIVATE_CONNECTION_TRANSFER("PVCOTR"),
//    /* PVCOACC */ 	CLIENT_PRIVATE_CONNECTION_CONFIRM("PVCOACC"),
//    /* PVCORES */ 	SERVER_PRIVATE_CONNECTION_RESPONSE("PVCORES"),
//    /* PVCOETA */ 	SERVER_PRIVATE_CONNECTION_ETABLISHMENT("PVCOETA"),
//    /* PVMSG */ 	CLIENT_PRIVATE_MESSAGE("PVSMG"),
//    /* PVFILE */ 	CLIENT_PRIVATE_FILE("PVFILE"),
//    /* PVDISCO */ 	CLIENT_PRIVATE_DISCONNECTION("PVDISCO"),
//    /* DISCO */ 	CLIENT_PUBLIC_DISCONNECTION("DISCO"),
    ;
    
    static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final String code;
    abstract ByteBuffer encodeProtocolMessage(Object[] args);
    abstract Object decodeProtocolMessage(ByteBuffer bb);
    
    private TestNetworkProtocol1(String code) {
	this.code = code;
    }
    
    public static ByteBuffer encodeMessage(TestNetworkProtocol1 protocol, Object[] objects) {
	return protocol.encodeProtocolMessage(objects);
    }
   
    public static Object decodeMessage(ByteBuffer bb) {
	bb.flip();
	int ordinal = bb.getInt();
	return TestNetworkProtocol1.values()[ordinal].decodeProtocolMessage(bb);
    }
    
    @Override
    public String toString() {
	return "[" + ordinal() + " - " + code + "]";
    }
    
    public static TestNetworkProtocol1 valueOf (int ordinal) {
	return values()[ordinal];
    }
    
    static ByteBuffer getBytes(ByteBuffer src, int size) {
	ByteBuffer dst = ByteBuffer.allocate(size);
	int position = src.position();
	int oldLimit = src.limit();
	src.limit(position + size);
	dst.put(src);
	src.limit(oldLimit);
	return dst;
    }
}
