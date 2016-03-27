package fr.upem.matou.nonblocking.test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class NetworkServerCommunication {
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	// bb should be flipped
	public static String readStringUTF8(ByteBuffer bb) {
		return UTF8_CHARSET.decode(bb).toString();
	}

	public static ByteBuffer encodeRequestCORES(boolean acceptation) {
		int capacity = Integer.BYTES + Byte.BYTES;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.SERVER_PUBLIC_CONNECTION_RESPONSE.ordinal());

		if (acceptation) {
			request.put((byte) 1);
		} else {
			request.put((byte) 0);
		}

		return request;
	}
	
	public static ByteBuffer encodeRequestMSGBC(String pseudo, String message) {
		ByteBuffer encodedPseudo = UTF8_CHARSET.encode(pseudo);
		ByteBuffer encodedMessage = UTF8_CHARSET.encode(message);

		int sizePseudo = encodedPseudo.remaining();
		int sizeMessage = encodedMessage.remaining();

		int capacity = Integer.BYTES + Integer.BYTES + sizePseudo + Integer.BYTES + sizeMessage;
		ByteBuffer request = ByteBuffer.allocate(capacity);

		request.putInt(NetworkProtocol.SERVER_PUBLIC_MESSAGE_BROADCAST.ordinal());
		request.putInt(sizePseudo).put(encodedPseudo);
		request.putInt(sizeMessage).put(encodedMessage);

		return request;
	}
}
