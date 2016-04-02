package fr.upem.matou.shared.network;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.matou.logger.Logger;

/*
 * This class gathers the common factors between ClientCommunication and ServerCommunication.
 */
public class NetworkCommunication {
	private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
	
	static final int LENGTH_SIZE = Integer.BYTES;
	static final int PSEUDO_MAX_SIZE = 32;
	static final int MESSAGE_MAX_SIZE = 512;

	private NetworkCommunication() {
	}

	private static boolean isValidPseudoCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint);
	}

	private static boolean isValidMessageCharacter(int codePoint) {
		return !Character.isISOControl(codePoint);
	}

	public static boolean checkPseudoValidity(String pseudo) {
		return pseudo.chars().allMatch(NetworkCommunication::isValidPseudoCharacter);
	}

	public static boolean checkMessageValidity(String message) {
		return message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}
	
	public static boolean checkEncodedPseudoValidity(ByteBuffer pseudo) {
		int size = pseudo.remaining();
		return size <= PSEUDO_MAX_SIZE;
	}
	
	public static boolean checkEncodedMessageValidity(ByteBuffer message) {
		int size = message.remaining();
		return size <= MESSAGE_MAX_SIZE;
	}

	public static Charset getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}
	
	public static Optional<ByteBuffer> encodePseudo(String pseudo) {
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(pseudo);
		Logger.debug("ENCODED PSEUDO SIZE : " + encoded.remaining());
		if(encoded.remaining() > PSEUDO_MAX_SIZE) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}
	
	public static Optional<ByteBuffer> encodeMessage(String message) {
		ByteBuffer encoded = PROTOCOL_CHARSET.encode(message);
		Logger.debug("ENCODED MESSAGE SIZE : " + encoded.remaining());
		if(encoded.remaining() > MESSAGE_MAX_SIZE) {
			return Optional.empty();
		}
		return Optional.of(encoded);
	}
	
	public static int getPseudoMaxSize() {
		return PSEUDO_MAX_SIZE;
	}
	
	public static int getMessageMaxSize() {
		return MESSAGE_MAX_SIZE;
	}
}
