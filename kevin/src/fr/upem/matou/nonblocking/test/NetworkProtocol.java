package fr.upem.matou.nonblocking.test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public enum NetworkProtocol {
    /* COREQ */ CLIENT_PUBLIC_CONNECTION_REQUEST("COREQ"),
    /* MSG */ CLIENT_PUBLIC_MESSAGE("MSG"),
    ;

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final String code;

    private NetworkProtocol(String code) {
	this.code = code;
    }

    @Override
    public String toString() {
	return "[" + ordinal() + " - " + code + "]";
    }

    public static NetworkProtocol valueOf(int ordinal) {
	// XXX : tester ordinal
	return values()[ordinal];
    }

    public static ByteBuffer encodeRequestCOREQ(String pseudo) {
	ByteBuffer encodedPseudo = UTF8_CHARSET.encode(pseudo);

	int length = encodedPseudo.remaining();

	int capacity = Integer.BYTES + Integer.BYTES + length;
	ByteBuffer request = ByteBuffer.allocate(capacity);

	request.putInt(NetworkProtocol.CLIENT_PUBLIC_CONNECTION_REQUEST.ordinal());
	request.putInt(length).put(encodedPseudo);
	
	return request;
    }
    
    public static ByteBuffer encodeRequestMSG(String message) {
	ByteBuffer encodedMessage = UTF8_CHARSET.encode(message);

	int length = encodedMessage.remaining();

	int capacity = Integer.BYTES + Integer.BYTES + length;
	ByteBuffer request = ByteBuffer.allocate(capacity);

	request.putInt(NetworkProtocol.CLIENT_PUBLIC_MESSAGE.ordinal());
	request.putInt(length).put(encodedMessage);
	
	return request;
    }

}
