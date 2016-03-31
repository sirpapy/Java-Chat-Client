package fr.upem.matou.shared.network;

import java.nio.charset.Charset;

/*
 * This class gathers the common factors between ClientCommunication and ServerCommunication.
 */
public class NetworkCommunication {
	private static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
	private static final int PSEUDO_MAX_LENGTH = 32;
	private static final int MESSAGE_MAX_LENGTH = 512;

	private NetworkCommunication() {
	}

	private static boolean isValidPseudoCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint);
	}

	private static boolean isValidMessageCharacter(int codePoint) {
		return !Character.isISOControl(codePoint);
	}

	private static boolean isValidPseudoLength(String string) {
		int length = string.length();
		return length > 0 && length <= PSEUDO_MAX_LENGTH;
	}

	private static boolean isValidMessageLength(String string) {
		int length = string.length();
		return length > 0 && length <= MESSAGE_MAX_LENGTH;
	}

	public static boolean checkPseudoValidity(String pseudo) {
		return isValidPseudoLength(pseudo) && pseudo.chars().allMatch(NetworkCommunication::isValidPseudoCharacter);
	}

	public static boolean checkMessageValidity(String message) {
		return isValidMessageLength(message) && message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}

	public static Charset getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}
}
