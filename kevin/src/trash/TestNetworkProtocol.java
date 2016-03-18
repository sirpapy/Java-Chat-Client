package trash;

import java.nio.ByteBuffer;

public enum TestNetworkProtocol {
    CLIENT_PUBLIC_CONNECTION_REQUEST() {
	@Override
	ByteBuffer encodeProtocolMessage(Object[] objects) {
	    ByteBuffer bb = ByteBuffer.allocate(1024);
	    
	    int sizePseudo = (int) objects[0];
	    bb.putInt(sizePseudo);
	    
	    String pseudo = (String) objects[1];
	    // some encoding code here for the pseudo
	    
	    return bb;
	}

	@Override
	Object decodeProtocolMessage(ByteBuffer bb) {
	    int sizePseudo = bb.getInt();
	    
	    String pseudo = "";
	    // some decoding code here for the pseudo
	    
	    return pseudo;
	}
    },
    CLIENT_PUBLIC_MESSAGE() {
	@Override
	ByteBuffer encodeProtocolMessage(Object[] objects) {
	    ByteBuffer bb = ByteBuffer.allocate(1024);
	    
	    int sizePseudo = (int) objects[0];
	    bb.putInt(sizePseudo);
	    
	    String pseudo = (String) objects[1];
	    // some encoding code here for the pseudo
	    
	    int sizeMessage = (int) objects[2];
	    bb.putInt(sizeMessage);
	    
	    String message = (String) objects[3];
	    // some encoding code here for the message
	    
	    return bb;
	}

	@Override
	Object decodeProtocolMessage(ByteBuffer bb) {
	    int sizePseudo = bb.getInt();
	    
	    String pseudo = "";
	    // some decoding code here for the pseudo
	    
	    int sizeMessage = bb.getInt();
	    
	    String message = "";
	    // some decoding code here for the message
	    
	    return pseudo + " : " + message ;
	}
    };
    
    abstract ByteBuffer encodeProtocolMessage(Object[] objects);
    abstract Object decodeProtocolMessage(ByteBuffer bb);
    
    private TestNetworkProtocol() {

    }
    
    public static ByteBuffer encodeMessage(TestNetworkProtocol protocol, Object[] objects) {
	return protocol.encodeProtocolMessage(objects);
    }
   
    public static Object decodeMessage(ByteBuffer bb) {
	bb.flip();
	int ordinal = bb.getInt();
	return TestNetworkProtocol.values()[ordinal].decodeProtocolMessage(bb);
    }
    

}
